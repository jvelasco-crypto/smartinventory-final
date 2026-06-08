package ec.org.cedia.smartinventory.dto.response;

import ec.org.cedia.smartinventory.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PurchaseOrderResponseDTO {

    private Long id;
    private Long productId;
    private String productName;
    private Long supplierId;
    private String supplierName;
    private Integer quantity;
    private OrderStatus status;
    private boolean automatic;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDate estimatedDeliveryDate;
    private LocalDateTime receivedAt;
}
