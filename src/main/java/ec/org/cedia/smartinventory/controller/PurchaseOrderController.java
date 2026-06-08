package ec.org.cedia.smartinventory.controller;

import ec.org.cedia.smartinventory.dto.request.PurchaseOrderCancelDTO;
import ec.org.cedia.smartinventory.dto.request.PurchaseOrderConfirmDTO;
import ec.org.cedia.smartinventory.dto.request.PurchaseOrderRequestDTO;
import ec.org.cedia.smartinventory.dto.response.PurchaseOrderResponseDTO;
import ec.org.cedia.smartinventory.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/purchase-orders")
@RequiredArgsConstructor
@Tag(name = "Órdenes de Compra", description = "Gestión del ciclo de vida de órdenes de compra")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @Operation(summary = "Crear orden de compra manual")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Orden creada en estado DRAFT"),
        @ApiResponse(responseCode = "400", description = "Proveedor no vinculado al producto o datos inválidos"),
        @ApiResponse(responseCode = "404", description = "Producto o proveedor no encontrado")
    })
    @PostMapping
    public ResponseEntity<PurchaseOrderResponseDTO> crear(
            @Valid @RequestBody PurchaseOrderRequestDTO dto) {
        return ResponseEntity.status(201).body(purchaseOrderService.create(dto));
    }

    @Operation(summary = "Listar órdenes de compra", description = "Retorna todas las órdenes ordenadas por fecha descendente")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Lista obtenida") })
    @GetMapping
    public ResponseEntity<Page<PurchaseOrderResponseDTO>> listar(
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(purchaseOrderService.list(pageable));
    }

    @Operation(summary = "Obtener orden por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orden encontrada"),
        @ApiResponse(responseCode = "404", description = "Orden no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderResponseDTO> obtener(
            @Parameter(description = "ID de la orden") @PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.getById(id));
    }

    @Operation(summary = "Enviar orden al proveedor (DRAFT → SENT)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orden enviada"),
        @ApiResponse(responseCode = "400", description = "Transición de estado inválida"),
        @ApiResponse(responseCode = "404", description = "Orden no encontrada")
    })
    @PatchMapping("/{id}/send")
    public ResponseEntity<PurchaseOrderResponseDTO> send(
            @Parameter(description = "ID de la orden") @PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.send(id));
    }

    @Operation(summary = "Confirmar orden (SENT → CONFIRMED)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orden confirmada con fecha estimada"),
        @ApiResponse(responseCode = "400", description = "Transición inválida o fecha faltante"),
        @ApiResponse(responseCode = "404", description = "Orden no encontrada")
    })
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<PurchaseOrderResponseDTO> confirm(
            @Parameter(description = "ID de la orden") @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderConfirmDTO dto) {
        return ResponseEntity.ok(purchaseOrderService.confirm(id, dto));
    }

    @Operation(summary = "Recibir orden (CONFIRMED → RECEIVED)", description = "Suma la cantidad al stock del producto")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orden recibida y stock actualizado"),
        @ApiResponse(responseCode = "400", description = "Transición inválida"),
        @ApiResponse(responseCode = "404", description = "Orden no encontrada")
    })
    @PatchMapping("/{id}/receive")
    public ResponseEntity<PurchaseOrderResponseDTO> receive(
            @Parameter(description = "ID de la orden") @PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.receive(id));
    }

    @Operation(summary = "Cancelar orden (DRAFT/SENT/CONFIRMED → CANCELLED)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orden cancelada"),
        @ApiResponse(responseCode = "400", description = "Transición inválida — no se puede cancelar RECEIVED"),
        @ApiResponse(responseCode = "404", description = "Orden no encontrada")
    })
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<PurchaseOrderResponseDTO> cancel(
            @Parameter(description = "ID de la orden") @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderCancelDTO dto) {
        return ResponseEntity.ok(purchaseOrderService.cancel(id, dto));
    }
}
