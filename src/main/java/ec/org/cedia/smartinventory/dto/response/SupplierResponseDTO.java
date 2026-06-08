package ec.org.cedia.smartinventory.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SupplierResponseDTO {

    private Long id;
    private String name;
    private Integer deliveryDays;
    private Boolean active;
}
