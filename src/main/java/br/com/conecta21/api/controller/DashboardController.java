package br.com.conecta21.api.controller;

import br.com.conecta21.api.dto.DashboardResponseDTO;
import br.com.conecta21.api.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardResponseDTO> obterMetricas() {
        DashboardResponseDTO metricas = dashboardService.obterMetricasUltimos30Dias();
        return ResponseEntity.ok(metricas);
    }
}
