package ec.org.cedia.smartinventory.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PurchaseOrderConfirmDTO {

    @NotNull(message = "La fecha estimada de entrega es obligatoria")
    private LocalDate estimatedDeliveryDate;
}
