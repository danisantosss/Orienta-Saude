package com.orientasaude.controller;

import com.orientasaude.dto.response.IndicadoresResponse;
import com.orientasaude.service.IndicadoresService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Área administrativa. A SegurancaConfig só deixa chegar aqui quem tem o papel ADMIN;
 * os demais recebem 403 antes do controller.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final IndicadoresService indicadoresService;

    /**
     * GET /api/admin/indicadores?inicio=aaaa-mm-dd&fim=aaaa-mm-dd
     * Indicadores agregados e anônimos do período (padrão: últimos 30 dias).
     */
    @GetMapping("/indicadores")
    public ResponseEntity<IndicadoresResponse> indicadores(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return ResponseEntity.ok(indicadoresService.calcular(inicio, fim));
    }
}
