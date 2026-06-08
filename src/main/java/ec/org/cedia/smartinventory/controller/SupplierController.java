package ec.org.cedia.smartinventory.controller;

import ec.org.cedia.smartinventory.dto.request.SupplierRequestDTO;
import ec.org.cedia.smartinventory.dto.response.SupplierResponseDTO;
import ec.org.cedia.smartinventory.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Proveedores", description = "Gestión de proveedores y vinculación con productos")
public class SupplierController {

    private final SupplierService supplierService;

    @Operation(summary = "Crear proveedor")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Proveedor creado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @PostMapping("/suppliers")
    public ResponseEntity<SupplierResponseDTO> crear(
            @Valid @RequestBody SupplierRequestDTO dto) {
        return ResponseEntity.status(201).body(supplierService.crear(dto));
    }

    @Operation(summary = "Listar proveedores")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Lista obtenida") })
    @GetMapping("/suppliers")
    public ResponseEntity<List<SupplierResponseDTO>> listar() {
        return ResponseEntity.ok(supplierService.listar());
    }

    @Operation(summary = "Vincular proveedor a producto")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Proveedor vinculado"),
        @ApiResponse(responseCode = "404", description = "Producto o proveedor no encontrado")
    })
    @PostMapping("/products/{productId}/suppliers/{supplierId}")
    public ResponseEntity<SupplierResponseDTO> vincular(
            @Parameter(description = "ID del producto") @PathVariable Long productId,
            @Parameter(description = "ID del proveedor") @PathVariable Long supplierId) {
        return ResponseEntity.ok(supplierService.vincularAProducto(productId, supplierId));
    }
}
