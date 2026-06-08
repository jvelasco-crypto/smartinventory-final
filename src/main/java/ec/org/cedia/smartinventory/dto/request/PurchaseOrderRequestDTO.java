package ec.org.cedia.smartinventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PurchaseOrderRequestDTO {

    @NotNull(message = "El ID del producto es obligatorio")
    private Long productId;

    @NotNull(message = "El ID del proveedor es obligatorio")
    private Long supplierId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer quantity;
}
