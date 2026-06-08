package ec.org.cedia.smartinventory.controller;

import ec.org.cedia.smartinventory.dto.response.PendingOrderReportDTO;
import ec.org.cedia.smartinventory.dto.response.SupplierDeliveryAvgDTO;
import ec.org.cedia.smartinventory.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Reportes de gestión de compras y proveedores")
public class ReportController {

    private final PurchaseOrderService purchaseOrderService;

    @Operation(summary = "Órdenes pendientes", description = "Lista órdenes en estado SENT o CONFIRMED con los días que llevan pendientes")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Reporte generado") })
    @GetMapping("/pending-orders")
    public ResponseEntity<List<PendingOrderReportDTO>> pendingOrders() {
        return ResponseEntity.ok(purchaseOrderService.getPendingOrdersReport());
    }

    @Operation(summary = "Promedio de entrega por proveedor", description = "Calcula el promedio de días entre creación y recepción de órdenes, agrupado por proveedor")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Reporte generado") })
    @GetMapping("/supplier-delivery-avg")
    public ResponseEntity<List<SupplierDeliveryAvgDTO>> supplierDeliveryAvg() {
        return ResponseEntity.ok(purchaseOrderService.getSupplierDeliveryAvg());
    }
}
