package custompage.sales.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import custompage.sales.dto.DetalleOrdenDTO;
import custompage.sales.dto.OrdenDTO;
import custompage.sales.dto.VentaRealizadaEventDTO;
import custompage.sales.exception.ResourceNotFoundException;
import custompage.sales.model.DetalleOrden;
import custompage.sales.model.Orden;
import custompage.sales.model.Venta;
import custompage.sales.repository.OrdenRepository;
import custompage.sales.repository.VentaRepository;
import custompage.sales.service.IOrdenService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrdenServiceImpl implements IOrdenService {

    private final OrdenRepository ordenRepository;
    private final VentaRepository ventaRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper; // 👈 Agregado para convertir el DTO a JSON String real

    public OrdenServiceImpl(OrdenRepository ordenRepository,
                            VentaRepository ventaRepository,
                            KafkaTemplate<String, String> kafkaTemplate,
                            ObjectMapper objectMapper) { // 👈 Inyectado en el constructor
        this.ordenRepository = ordenRepository;
        this.ventaRepository = ventaRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public OrdenDTO generarOrden(OrdenDTO dto) {
        Orden orden = Orden.builder()
                .idEmpresa(dto.getIdEmpresa())
                .fechaCreacion(LocalDateTime.now())
                .estado("PENDIENTE")
                .total(dto.getTotal())
                .build();

        List<DetalleOrden> detalles = dto.getDetalles().stream().map(d ->
                DetalleOrden.builder()
                        .orden(orden)
                        .codigoSKU(d.getCodigoSKU())
                        .cantidad(d.getCantidad())
                        .precioUnitario(d.getPrecioUnitario())
                        .build()
        ).collect(Collectors.toList());

        orden.setDetalles(detalles);
        Orden guardada = ordenRepository.save(orden);
        return convertToDTO(guardada);
    }

    @Override
    @Transactional
    public OrdenDTO procesarPagoYVenta(Long idOrden, String metodoPago) {
        Orden orden = ordenRepository.findById(idOrden)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + idOrden));

        if ("PAGADO".equals(orden.getEstado())) {
            throw new RuntimeException("La orden ya fue pagada anteriormente");
        }

        // 1. Actualizar estado
        orden.setEstado("PAGADO");
        ordenRepository.save(orden);

        // 2. Crear el registro en la tabla Venta
        Venta venta = Venta.builder()
                .orden(orden)
                .fechaTransaccion(LocalDateTime.now())
                .totalCobrado(orden.getTotal())
                .metodoPago(metodoPago)
                .build();
        Venta ventaGuardada = ventaRepository.save(venta);

        // 3. Notificar asíncronamente a través de Kafka al microservicio de Marketing (Mensaje JSON único)
        // Construimos el DTO estructurado con los datos reales necesarios para analítica y cupones
        VentaRealizadaEventDTO eventoMarketing = VentaRealizadaEventDTO.builder()
                .idVenta(ventaGuardada.getIdVenta())
                .numeroOrden("ORD-" + orden.getIdOrden()) // Generación de código de orden
                .idCliente(orden.getIdEmpresa()) // Usamos el ID ligado a la cuenta/empresa compradora
                .total(ventaGuardada.getTotalCobrado())
                .fechaVenta(ventaGuardada.getFechaTransaccion().toString())
                .build();

        try {
            // Convertimos el objeto de evento a un String JSON real
            String jsonMensaje = objectMapper.writeValueAsString(eventoMarketing);

            // Enviamos el JSON al tópico que escucha el microservicio de marketing
            kafkaTemplate.send("venta-realizada-topic", jsonMensaje);

            System.out.println("Mensaje JSON enviado con éxito a Kafka para Marketing: " + jsonMensaje);
        } catch (Exception e) {
            // Manejo de error de serialización local para no romper el flujo de la transacción de pago
            System.err.println("Error al serializar el evento de ventas para Kafka: " + e.getMessage());
        }

        return convertToDTO(orden);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenDTO> listarPorEmpresa(Long idEmpresa) {
        return ordenRepository.findByIdEmpresa(idEmpresa).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private OrdenDTO convertToDTO(Orden orden) {
        List<DetalleOrdenDTO> detallesDTO = orden.getDetalles().stream().map(d ->
                DetalleOrdenDTO.builder()
                        .codigoSKU(d.getCodigoSKU())
                        .cantidad(d.getCantidad())
                        .precioUnitario(d.getPrecioUnitario())
                        .build()
        ).collect(Collectors.toList());

        return OrdenDTO.builder()
                .idOrden(orden.getIdOrden())
                .idEmpresa(orden.getIdEmpresa())
                .estado(orden.getEstado())
                .total(orden.getTotal())
                .detalles(detallesDTO)
                .build();
    }
}