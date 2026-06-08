package ec.org.cedia.smartinventory.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductResponseDTO {

    private Long id;
    private String name;
    private String description;
    private Double price;
    private Integer stock;
    private Boolean active;
    private Integer minimumStock;
}