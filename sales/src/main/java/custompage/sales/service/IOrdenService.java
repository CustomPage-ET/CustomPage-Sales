package custompage.sales.service;

import custompage.sales.dto.OrdenDTO;
import java.util.List;

public interface IOrdenService {
    OrdenDTO generarOrden(OrdenDTO ordenDTO);
    OrdenDTO procesarPagoYVenta(Long idOrden, String metodoPago);
    List<OrdenDTO> listarPorEmpresa(Long idEmpresa);
}