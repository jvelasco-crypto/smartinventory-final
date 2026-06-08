package ec.org.cedia.smartinventory.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SupplierDeliveryAvgDTO {

    private Long supplierId;
    private String supplierName;
    private Double avgDeliveryDays;
    private Long totalReceived;
}
