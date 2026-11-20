package com.example.cargalectiva;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ciclos academicos del SIS Carga Lectiva (datos en memoria, sin base de datos).
 *
 *   GET  /api/ciclos-academicos   -> consulta de ciclos academicos
 *   POST /api/ciclos-academicos   -> registro de un nuevo ciclo academico
 */
@RestController
@RequestMapping("/api/ciclos-academicos")
public class CiclosAcademicosRestController {

    private final List<Map<String, Object>> ciclos = new ArrayList<>();

    public CiclosAcademicosRestController() {
        ciclos.add(ciclo("2025-II", "2025-08-18", "2025-12-19", "2025-07-15", "2025-08-15", "Cerrado"));
        ciclos.add(ciclo("2026-I", "2026-03-16", "2026-07-31", "2026-02-01", "2026-03-13", "Cerrado"));
        ciclos.add(ciclo("2026-II", "2026-08-17", "2026-12-18", "2026-07-01", "2026-07-31", "Activo"));
    }

    // GET: consultar ciclos academicos
    @GetMapping
    public synchronized List<Map<String, Object>> listar() {
        System.out.println(">>> GET /api/ciclos-academicos");
        return new ArrayList<>(ciclos);
    }

    // POST: registrar ciclo academico
    // Body: {"id":"2027-I","fechaInicio":"2027-03-15","fechaFin":"2027-07-30",
    //        "inicioPlanificacion":"2027-02-01","finPlanificacion":"2027-03-10"}
    @PostMapping
    public synchronized ResponseEntity<Map<String, Object>> registrar(@RequestBody Map<String, Object> datos) {
        System.out.println(">>> POST /api/ciclos-academicos " + datos);
        String id = texto(datos, "id").toUpperCase();
        List<String> errores = new ArrayList<>();

        if (!id.matches("\\d{4}-(I|II)")) errores.add("El ciclo debe tener el formato AAAA-I o AAAA-II.");
        if (existe(id)) errores.add("El ciclo " + id + " ya existe.");

        LocalDate inicio = fecha(datos, "fechaInicio", errores);
        LocalDate fin = fecha(datos, "fechaFin", errores);
        LocalDate planIni = fecha(datos, "inicioPlanificacion", errores);
        LocalDate planFin = fecha(datos, "finPlanificacion", errores);
        if (inicio != null && fin != null && !inicio.isBefore(fin))
            errores.add("El inicio de clases debe ser anterior al fin de clases.");
        if (planIni != null && planFin != null && !planIni.isBefore(planFin))
            errores.add("El inicio de planificacion debe ser anterior a su fin.");
        if (planFin != null && inicio != null && !planFin.isBefore(inicio))
            errores.add("La planificacion debe terminar antes del inicio de clases.");

        if (!errores.isEmpty()) return ResponseEntity.badRequest().body(respuesta(false, errores, null));

        Map<String, Object> nuevo = ciclo(id, inicio.toString(), fin.toString(),
                planIni.toString(), planFin.toString(), "Borrador");
        ciclos.add(nuevo);
        return ResponseEntity.status(201).body(respuesta(true, List.of("Ciclo " + id + " registrado."), nuevo));
    }

    /** Usado por AsignacionesRestController para validar que el ciclo exista. */
    public synchronized boolean existe(String id) {
        return ciclos.stream().anyMatch(c -> c.get("id").equals(id));
    }

    private static Map<String, Object> ciclo(String id, String inicio, String fin,
                                             String planIni, String planFin, String estado) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("id", id);
        c.put("descripcion", "Ciclo Academico " + id);
        c.put("fechaInicio", inicio);
        c.put("fechaFin", fin);
        c.put("inicioPlanificacion", planIni);
        c.put("finPlanificacion", planFin);
        c.put("estado", estado);
        return c;
    }

    private static Map<String, Object> respuesta(boolean ok, List<String> mensajes, Object datos) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", ok);
        r.put("mensaje", String.join(" ", mensajes));
        if (datos != null) r.put("datos", datos);
        return r;
    }

    private static String texto(Map<String, Object> m, String clave) {
        Object v = m.get(clave);
        return v == null ? "" : v.toString().trim();
    }

    private static LocalDate fecha(Map<String, Object> m, String clave, List<String> errores) {
        try {
            return LocalDate.parse(texto(m, clave));
        } catch (Exception e) {
            errores.add(clave + " debe ser una fecha valida (AAAA-MM-DD).");
            return null;
        }
    }
}
