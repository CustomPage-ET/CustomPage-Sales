package custompage.sales.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenDTO {

    private Long idOrden;

    @NotNull(message = "El ID de la empresa es obligatorio")
    private Long idEmpresa;

    private String estado;

    @NotNull(message = "El total de la orden es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El total no puede ser negativo")
    private BigDecimal total;

    @NotEmpty(message = "El carrito de compras no puede estar vacío")
    private List<DetalleOrdenDTO> detalles;
}
