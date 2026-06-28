package custompage.sales.controller;

import custompage.sales.dto.OrdenDTO;
import custompage.sales.service.IOrdenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrdenController {

    private final IOrdenService ordenService;

    public OrdenController(IOrdenService ordenService) {
        this.ordenService = ordenService;
    }

    @PostMapping
    public ResponseEntity<OrdenDTO> crearOrden(@Validated @RequestBody OrdenDTO ordenDTO) {
        OrdenDTO creada = ordenService.generarOrden(ordenDTO);
        return new ResponseEntity<>(creada, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<OrdenDTO> pagarOrden(@PathVariable Long id, @RequestParam String metodoPago) {
        OrdenDTO pagada = ordenService.procesarPagoYVenta(id, metodoPago);
        return ResponseEntity.ok(pagada);
    }

    @GetMapping("/empresa/{idEmpresa}")
    public ResponseEntity<List<OrdenDTO>> listarPorPYME(@PathVariable Long idEmpresa) {
        return ResponseEntity.ok(ordenService.listarPorEmpresa(idEmpresa));
    }
}