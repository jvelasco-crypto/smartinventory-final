package ec.org.cedia.smartinventory.service;

import ec.org.cedia.smartinventory.dto.request.PurchaseOrderCancelDTO;
import ec.org.cedia.smartinventory.dto.request.PurchaseOrderConfirmDTO;
import ec.org.cedia.smartinventory.dto.request.PurchaseOrderRequestDTO;
import ec.org.cedia.smartinventory.dto.response.PendingOrderReportDTO;
import ec.org.cedia.smartinventory.dto.response.PurchaseOrderResponseDTO;
import ec.org.cedia.smartinventory.dto.response.SupplierDeliveryAvgDTO;
import ec.org.cedia.smartinventory.enums.OrderStatus;
import ec.org.cedia.smartinventory.exception.BusinessException;
import ec.org.cedia.smartinventory.exception.ResourceNotFoundException;
import ec.org.cedia.smartinventory.model.Product;
import ec.org.cedia.smartinventory.model.PurchaseOrder;
import ec.org.cedia.smartinventory.model.Supplier;
import ec.org.cedia.smartinventory.repository.ProductRepository;
import ec.org.cedia.smartinventory.repository.PurchaseOrderRepository;
import ec.org.cedia.smartinventory.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            OrderStatus.DRAFT,     Set.of(OrderStatus.SENT, OrderStatus.CANCELLED),
            OrderStatus.SENT,      Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.RECEIVED, OrderStatus.CANCELLED),
            OrderStatus.RECEIVED,  Set.of(),
            OrderStatus.CANCELLED, Set.of()
    );

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;

    public PurchaseOrderResponseDTO create(PurchaseOrderRequestDTO dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + dto.getProductId()));
        Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con id: " + dto.getSupplierId()));

        boolean vinculado = product.getSuppliers().stream()
                .anyMatch(s -> s.getId().equals(supplier.getId()));
        if (!vinculado) {
            throw new BusinessException("El proveedor id=" + supplier.getId() + " no está vinculado al producto id=" + product.getId());
        }

        PurchaseOrder order = PurchaseOrder.builder()
                .product(product)
                .supplier(supplier)
                .quantity(dto.getQuantity())
                .status(OrderStatus.DRAFT)
                .automatic(false)
                .build();

        PurchaseOrder saved = purchaseOrderRepository.save(order);
        log.info("Orden de compra creada: id={}, producto='{}', proveedor='{}'",
                saved.getId(), product.getName(), supplier.getName());
        return toResponseDTO(saved);
    }

    public void createAutomaticIfNeeded(Product product) {
        boolean ordenActiva = purchaseOrderRepository.existsByProductIdAndStatusIn(
                product.getId(), List.of(OrderStatus.DRAFT, OrderStatus.SENT, OrderStatus.CONFIRMED));
        if (ordenActiva) {
            log.debug("Ya existe orden activa para producto id={} — no se crea automática", product.getId());
            return;
        }

        Supplier supplier = product.getSuppliers().stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive()))
                .min(Comparator.comparingInt(Supplier::getDeliveryDays))
                .orElse(null);

        if (supplier == null) {
            log.warn("Producto id={} sin proveedores activos — no se crea orden automática", product.getId());
            return;
        }

        int quantity = product.getMinimumStock() != null ? product.getMinimumStock() * 2 : 1;
        PurchaseOrder order = PurchaseOrder.builder()
                .product(product)
                .supplier(supplier)
                .quantity(quantity)
                .status(OrderStatus.DRAFT)
                .automatic(true)
                .build();

        PurchaseOrder saved = purchaseOrderRepository.save(order);
        log.info("Orden automática creada: id={}, producto='{}', proveedor='{}', cantidad={}",
                saved.getId(), product.getName(), supplier.getName(), quantity);
    }

    public PurchaseOrderResponseDTO send(Long id) {
        PurchaseOrder order = getOrderOrThrow(id);
        validateTransition(order.getStatus(), OrderStatus.SENT);
        order.setStatus(OrderStatus.SENT);
        PurchaseOrder saved = purchaseOrderRepository.save(order);
        log.info("Orden id={} enviada al proveedor", id);
        return toResponseDTO(saved);
    }

    public PurchaseOrderResponseDTO confirm(Long id, PurchaseOrderConfirmDTO dto) {
        PurchaseOrder order = getOrderOrThrow(id);
        validateTransition(order.getStatus(), OrderStatus.CONFIRMED);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setEstimatedDeliveryDate(dto.getEstimatedDeliveryDate());
        PurchaseOrder saved = purchaseOrderRepository.save(order);
        log.info("Orden id={} confirmada, entrega estimada={}", id, dto.getEstimatedDeliveryDate());
        return toResponseDTO(saved);
    }

    @Transactional
    public PurchaseOrderResponseDTO receive(Long id) {
        PurchaseOrder order = getOrderOrThrow(id);
        validateTransition(order.getStatus(), OrderStatus.RECEIVED);

        order.setStatus(OrderStatus.RECEIVED);
        order.setReceivedAt(LocalDateTime.now());

        Product product = order.getProduct();
        int stockActual = product.getStock() != null ? product.getStock() : 0;
        product.setStock(stockActual + order.getQuantity());
        productRepository.save(product);

        PurchaseOrder saved = purchaseOrderRepository.save(order);
        log.info("Orden id={} recibida — stock de producto id={} actualizado a {}",
                id, product.getId(), product.getStock());
        return toResponseDTO(saved);
    }

    public PurchaseOrderResponseDTO cancel(Long id, PurchaseOrderCancelDTO dto) {
        PurchaseOrder order = getOrderOrThrow(id);
        validateTransition(order.getStatus(), OrderStatus.CANCELLED);
        order.setStatus(OrderStatus.CANCELLED);
        order.setReason(dto.getReason());
        PurchaseOrder saved = purchaseOrderRepository.save(order);
        log.info("Orden id={} cancelada. Motivo: '{}'", id, dto.getReason());
        return toResponseDTO(saved);
    }

    public Page<PurchaseOrderResponseDTO> list(Pageable pageable) {
        return purchaseOrderRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponseDTO);
    }

    public PurchaseOrderResponseDTO getById(Long id) {
        return toResponseDTO(getOrderOrThrow(id));
    }

    public List<PendingOrderReportDTO> getPendingOrdersReport() {
        return purchaseOrderRepository.findPendingOrders().stream()
                .map(order -> PendingOrderReportDTO.builder()
                        .orderId(order.getId())
                        .productName(order.getProduct().getName())
                        .supplierName(order.getSupplier().getName())
                        .quantity(order.getQuantity())
                        .status(order.getStatus())
                        .daysPending(ChronoUnit.DAYS.between(order.getCreatedAt(), LocalDateTime.now()))
                        .estimatedDeliveryDate(order.getEstimatedDeliveryDate())
                        .build())
                .toList();
    }

    public List<SupplierDeliveryAvgDTO> getSupplierDeliveryAvg() {
        List<PurchaseOrder> received = purchaseOrderRepository.findByStatus(OrderStatus.RECEIVED);

        return received.stream()
                .collect(Collectors.groupingBy(o -> o.getSupplier().getId()))
                .entrySet().stream()
                .map(entry -> {
                    List<PurchaseOrder> orders = entry.getValue();
                    Supplier supplier = orders.get(0).getSupplier();
                    double avg = orders.stream()
                            .filter(o -> o.getReceivedAt() != null)
                            .mapToLong(o -> ChronoUnit.DAYS.between(o.getCreatedAt(), o.getReceivedAt()))
                            .average()
                            .orElse(0.0);
                    return SupplierDeliveryAvgDTO.builder()
                            .supplierId(supplier.getId())
                            .supplierName(supplier.getName())
                            .avgDeliveryDays(avg)
                            .totalReceived((long) orders.size())
                            .build();
                })
                .toList();
    }

    private void validateTransition(OrderStatus from, OrderStatus to) {
        Set<OrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new BusinessException("Transición inválida: " + from + " → " + to);
        }
    }

    private PurchaseOrder getOrderOrThrow(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de compra no encontrada con id: " + id));
    }

    private PurchaseOrderResponseDTO toResponseDTO(PurchaseOrder order) {
        return PurchaseOrderResponseDTO.builder()
                .id(order.getId())
                .productId(order.getProduct().getId())
                .productName(order.getProduct().getName())
                .supplierId(order.getSupplier().getId())
                .supplierName(order.getSupplier().getName())
                .quantity(order.getQuantity())
                .status(order.getStatus())
                .automatic(order.isAutomatic())
                .reason(order.getReason())
                .createdAt(order.getCreatedAt())
                .estimatedDeliveryDate(order.getEstimatedDeliveryDate())
                .receivedAt(order.getReceivedAt())
                .build();
    }
}
