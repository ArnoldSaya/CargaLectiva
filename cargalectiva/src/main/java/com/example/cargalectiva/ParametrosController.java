package com.example.cargalectiva;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ParametrosController {

    @GetMapping("/parametros")
    public Map<String, Object> parametros() {
        return Map.of(
            "MIN_HORAS_DOCENTE", 12,
            "MAX_HORAS_DOCENTE", 20,
            "CICLO_VIGENTE", "2026-II",
            "ID_CARPETA_PLANTILLAS", "1AbCdEfGhIjKlMnOpQrStUvWxYz"
        );
    }
}