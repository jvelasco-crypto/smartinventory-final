package ec.org.cedia.smartinventory.service;

import ec.org.cedia.smartinventory.dto.request.PurchaseOrderCancelDTO;
import ec.org.cedia.smartinventory.enums.OrderStatus;
import ec.org.cedia.smartinventory.exception.BusinessException;
import ec.org.cedia.smartinventory.model.Product;
import ec.org.cedia.smartinventory.model.PurchaseOrder;
import ec.org.cedia.smartinventory.model.Supplier;
import ec.org.cedia.smartinventory.repository.ProductRepository;
import ec.org.cedia.smartinventory.repository.PurchaseOrderRepository;
import ec.org.cedia.smartinventory.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private SupplierRepository supplierRepository;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    private Supplier activeSupplier;
    private Product product;

    @BeforeEach
    void setUp() {
        activeSupplier = Supplier.builder()
                .id(1L)
                .name("Proveedor A")
                .deliveryDays(3)
                .active(true)
                .build();

        product = Product.builder()
                .id(1L)
                .name("Laptop")
                .stock(2)
                .minimumStock(5)
                .suppliers(new java.util.HashSet<>(Set.of(activeSupplier)))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────
    // createAutomaticIfNeeded — sin orden activa → crea DRAFT automática
    // ─────────────────────────────────────────────────────────────────────
    @Test
    void createAutomaticIfNeeded_sinOrdenActiva_creaDraft() {
        when(purchaseOrderRepository.existsByProductIdAndStatusIn(eq(1L), anyList())).thenReturn(false);
        when(purchaseOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        purchaseOrderService.createAutomaticIfNeeded(product);

        verify(purchaseOrderRepository).save(argThat(order ->
                order.isAutomatic()
                && order.getStatus() == OrderStatus.DRAFT
                && order.getQuantity() == 10   // minimumStock(5) * 2
                && order.getSupplier().equals(activeSupplier)
        ));
    }

    // ─────────────────────────────────────────────────────────────────────
    // createAutomaticIfNeeded — ya hay orden activa → no crea nada
    // ─────────────────────────────────────────────────────────────────────
    @Test
    void createAutomaticIfNeeded_conOrdenActiva_noCrea() {
        when(purchaseOrderRepository.existsByProductIdAndStatusIn(eq(1L), anyList())).thenReturn(true);

        purchaseOrderService.createAutomaticIfNeeded(product);

        verify(purchaseOrderRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────
    // createAutomaticIfNeeded — sin proveedor activo → no crea nada
    // ─────────────────────────────────────────────────────────────────────
    @Test
    void createAutomaticIfNeeded_sinProveedor_noCrea() {
        product.setSuppliers(new java.util.HashSet<>());
        when(purchaseOrderRepository.existsByProductIdAndStatusIn(eq(1L), anyList())).thenReturn(false);

        purchaseOrderService.createAutomaticIfNeeded(product);

        verify(purchaseOrderRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────
    // receive — suma quantity al stock y marca receivedAt
    // ─────────────────────────────────────────────────────────────────────
    @Test
    void receive_sumaStockYMarcaRecibida() {
        PurchaseOrder order = PurchaseOrder.builder()
                .id(1L)
                .product(product)
                .supplier(activeSupplier)
                .quantity(5)
                .status(OrderStatus.CONFIRMED)
                .automatic(false)
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        purchaseOrderService.receive(1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(order.getReceivedAt()).isNotNull();
        assertThat(product.getStock()).isEqualTo(7); // 2 + 5
    }

    // ─────────────────────────────────────────────────────────────────────
    // cancel sobre orden RECEIVED → BusinessException (terminal state)
    // ─────────────────────────────────────────────────────────────────────
    @Test
    void cancel_sobreRecibida_lanzaBusinessException() {
        PurchaseOrder order = PurchaseOrder.builder()
                .id(1L)
                .product(product)
                .supplier(activeSupplier)
                .quantity(5)
                .status(OrderStatus.RECEIVED)
                .automatic(false)
                .createdAt(LocalDateTime.now().minusDays(3))
                .build();

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        PurchaseOrderCancelDTO dto = new PurchaseOrderCancelDTO();
        dto.setReason("Intento de cancelación de orden ya recibida");

        assertThatThrownBy(() -> purchaseOrderService.cancel(1L, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("RECEIVED");
    }
}
