package ec.org.cedia.smartinventory.dto.response;

import ec.org.cedia.smartinventory.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class PendingOrderReportDTO {

    private Long orderId;
    private String productName;
    private String supplierName;
    private Integer quantity;
    private OrderStatus status;
    private Long daysPending;
    private LocalDate estimatedDeliveryDate;
}
