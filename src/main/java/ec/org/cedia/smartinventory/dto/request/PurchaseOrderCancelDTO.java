package ec.org.cedia.smartinventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PurchaseOrderCancelDTO {

    @NotBlank(message = "El motivo de cancelación es obligatorio")
    @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
    private String reason;
}
