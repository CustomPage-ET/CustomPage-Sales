package custompage.sales.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaRealizadaEventDTO {
    private Long idVenta;
    private String numeroOrden;
    private Long idCliente;
    private BigDecimal total;
    private String fechaVenta;
}