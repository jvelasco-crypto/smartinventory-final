package ec.org.cedia.smartinventory.service;

import ec.org.cedia.smartinventory.dto.request.SupplierRequestDTO;
import ec.org.cedia.smartinventory.dto.response.SupplierResponseDTO;
import ec.org.cedia.smartinventory.exception.ResourceNotFoundException;
import ec.org.cedia.smartinventory.model.Product;
import ec.org.cedia.smartinventory.model.Supplier;
import ec.org.cedia.smartinventory.repository.ProductRepository;
import ec.org.cedia.smartinventory.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;

    public SupplierResponseDTO crear(SupplierRequestDTO dto) {
        Supplier supplier = Supplier.builder()
                .name(dto.getName())
                .deliveryDays(dto.getDeliveryDays())
                .active(true)
                .build();
        Supplier saved = supplierRepository.save(supplier);
        log.info("Proveedor creado: id={}, nombre='{}'", saved.getId(), saved.getName());
        return toResponseDTO(saved);
    }

    public List<SupplierResponseDTO> listar() {
        return supplierRepository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional
    public SupplierResponseDTO vincularAProducto(Long productId, Long supplierId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productId));
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con id: " + supplierId));

        product.getSuppliers().add(supplier);
        productRepository.save(product);
        log.info("Proveedor id={} vinculado al producto id={}", supplierId, productId);
        return toResponseDTO(supplier);
    }

    private SupplierResponseDTO toResponseDTO(Supplier s) {
        return SupplierResponseDTO.builder()
                .id(s.getId())
                .name(s.getName())
                .deliveryDays(s.getDeliveryDays())
                .active(s.getActive())
                .build();
    }
}
