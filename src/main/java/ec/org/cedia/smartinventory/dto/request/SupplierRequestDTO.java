package ec.org.cedia.smartinventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SupplierRequestDTO {

    @NotBlank(message = "El nombre del proveedor es obligatorio")
    private String name;

    @NotNull(message = "Los días de entrega son obligatorios")
    @Min(value = 0, message = "Los días de entrega no pueden ser negativos")
    private Integer deliveryDays;
}
