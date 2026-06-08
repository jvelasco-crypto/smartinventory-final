package ec.org.cedia.smartinventory.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ec.org.cedia.smartinventory.dto.ProductRequestDTO;
import ec.org.cedia.smartinventory.dto.ProductResponseDTO;
import ec.org.cedia.smartinventory.exception.ResourceNotFoundException;
import ec.org.cedia.smartinventory.model.Product;
import ec.org.cedia.smartinventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Esta clase se encarga de gestionar las operaciones matemáticas básicas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService implements ProductUseCase {

    private final ProductRepository productRepository;
    private final PurchaseOrderService purchaseOrderService;

    /**
     * Crea un nuevo producto en el inventario.
     * 
     * @param request El objeto que contiene los datos del producto a crear.
     * @return El producto creado con su ID asignado.
     */
    @Override
    public ProductResponseDTO crearProducto(ProductRequestDTO request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .active(true)
                .build();
        return toResponseDTO(productRepository.save(product));
    }

    @Override
    public Page<ProductResponseDTO> listarProductos(Pageable pageable) {
        log.debug("Listando — página={}, tamaño={}", pageable.getPageNumber(), pageable.getPageSize());
        return productRepository.findAll(pageable).map(this::toResponseDTO);
    }

    @Override
    public ProductResponseDTO obtenerProducto(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
        return toResponseDTO(product);
    }

    @Override
    @Transactional
    public ProductResponseDTO actualizarProducto(Long id, ProductRequestDTO request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        Product saved = productRepository.save(product);

        if (saved.getMinimumStock() != null && saved.getStock() != null
                && saved.getStock() <= saved.getMinimumStock()) {
            log.info("Stock ({}) ≤ minimumStock ({}) para producto id={} — disparando orden automática",
                    saved.getStock(), saved.getMinimumStock(), saved.getId());
            purchaseOrderService.createAutomaticIfNeeded(saved);
        }

        return toResponseDTO(saved);
    }

    @Override
    public void eliminarProducto(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Producto no encontrado con id: " + id);
        }
        productRepository.deleteById(id);
    }

    private ProductResponseDTO toResponseDTO(Product product) {
        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .active(product.getActive())
                .build();
    }
}
