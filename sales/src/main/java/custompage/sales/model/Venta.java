package custompage.sales.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ventas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idVenta;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_orden", nullable = false, unique = true)
    private Orden orden;

    @Column(nullable = false)
    private LocalDateTime fechaTransaccion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalCobrado;

    @Column(nullable = false, length = 50)
    private String metodoPago; // Ejemplo: Webpay, Transferencia
}