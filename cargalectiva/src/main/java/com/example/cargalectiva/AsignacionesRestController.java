package com.example.cargalectiva;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Asignaciones de carga docente: que docente dicta que curso, en que ciclo,
 * con cuantas horas (datos en memoria, sin base de datos).
 *
 *   GET  /api/asignaciones?ciclo=2026-II&docente=Quispe   -> consulta (filtros opcionales)
 *   POST /api/asignaciones                                -> registrar una asignacion
 */
@RestController
@RequestMapping("/api/asignaciones")
public class AsignacionesRestController {

    /** Tope de horas por docente y ciclo usado para validar la asignacion. */
    private static final int TOPE_HORAS_DOCENTE = 24;
    private static final List<String> TURNOS = List.of("Mañana", "Tarde", "Noche");

    private final CiclosAcademicosRestController ciclos;
    private final List<Map<String, Object>> asignaciones = new ArrayList<>();
    private final AtomicInteger secuencia = new AtomicInteger(1000);

    public AsignacionesRestController(CiclosAcademicosRestController ciclos) {
        this.ciclos = ciclos;
        asignaciones.add(asignacion("2026-II", "Ing. Software", 8, "IS801", "Gestion de la Configuracion y Cambio", "A", 2, 2, "Quispe Mamani, Luis", "DIMA", "Noche"));
        asignaciones.add(asignacion("2026-II", "Ing. Software", 8, "IS802", "Arquitectura de Software", "A", 2, 2, "Quispe Mamani, Luis", "DIMA", "Noche"));
        asignaciones.add(asignacion("2026-II", "Ing. Software", 6, "IS603", "Base de Datos II", "A", 2, 4, "Flores Huaman, Rosa", "DIMA", "Tarde"));
        asignaciones.add(asignacion("2026-II", "Ing. Industrial", 4, "II401", "Investigacion de Operaciones", "B", 3, 2, "Torres Vargas, Jorge", "DIMA", "Mañana"));
        asignaciones.add(asignacion("2026-II", "Derecho", 2, "DE201", "Derecho Civil I", "A", 4, 0, "Salas Ccori, Ana", "DACJ", "Mañana"));
    }

    // GET: consultar asignaciones, con filtros opcionales por ciclo y docente
    @GetMapping
    public synchronized List<Map<String, Object>> listar(
            @RequestParam(required = false) String ciclo,
            @RequestParam(required = false) String docente) {
        System.out.println(">>> GET /api/asignaciones ciclo=" + ciclo + " docente=" + docente);
        return asignaciones.stream()
                .filter(a -> ciclo == null || ciclo.isBlank() || a.get("ciclo").equals(ciclo))
                .filter(a -> docente == null || docente.isBlank()
                        || a.get("docente").toString().toLowerCase().contains(docente.toLowerCase()))
                .toList();
    }

    // POST: registrar asignacion de un curso a un docente
    // Body: {"ciclo":"2026-II","carrera":"Ing. Software","semestre":8,"codigoCurso":"IS803",
    //        "asignatura":"Calidad de Software","grupo":"A","horasTeoria":2,"horasPractica":2,
    //        "docente":"Flores Huaman, Rosa","departamento":"DIMA","turno":"Noche"}
    @PostMapping
    public synchronized ResponseEntity<Map<String, Object>> registrar(@RequestBody Map<String, Object> d) {
        System.out.println(">>> POST /api/asignaciones " + d);
        List<String> errores = new ArrayList<>();

        String ciclo = texto(d, "ciclo").toUpperCase();
        String carrera = texto(d, "carrera");
        String codigo = texto(d, "codigoCurso").toUpperCase();
        String asignatura = texto(d, "asignatura");
        String grupo = texto(d, "grupo").toUpperCase();
        String docente = texto(d, "docente");
        String dpto = texto(d, "departamento").toUpperCase();
        String turno = texto(d, "turno");
        Integer semestre = entero(d.get("semestre"));
        Integer ht = entero(d.get("horasTeoria"));
        Integer hp = entero(d.get("horasPractica"));

        if (!ciclos.existe(ciclo)) errores.add("El ciclo " + ciclo + " no esta registrado.");
        if (carrera.isEmpty()) errores.add("Indique la carrera.");
        if (codigo.isEmpty()) errores.add("Indique el codigo del curso.");
        if (asignatura.isEmpty()) errores.add("Indique la asignatura.");
        if (grupo.isEmpty()) errores.add("Indique el grupo.");
        if (docente.isEmpty()) errores.add("Indique el docente.");
        if (dpto.isEmpty()) errores.add("Indique el departamento academico.");
        if (!TURNOS.contains(turno)) errores.add("El turno debe ser Mañana, Tarde o Noche.");
        if (semestre == null || semestre < 1 || semestre > 12) errores.add("El semestre debe estar entre 1 y 12.");
        if (ht == null || ht < 0 || hp == null || hp < 0) errores.add("Las horas de teoria y practica deben ser enteros >= 0.");
        else if (ht + hp == 0) errores.add("El curso debe tener al menos 1 hora.");

        boolean duplicado = asignaciones.stream().anyMatch(a ->
                a.get("ciclo").equals(ciclo) && a.get("codigoCurso").equals(codigo) && a.get("grupo").equals(grupo));
        if (duplicado) errores.add("El curso " + codigo + " grupo " + grupo + " ya tiene docente en " + ciclo + ".");

        if (errores.isEmpty()) {
            int horasActuales = horasDocente(ciclo, docente);
            int total = horasActuales + ht + hp;
            if (total > TOPE_HORAS_DOCENTE) {
                errores.add(docente + " ya tiene " + horasActuales + " h en " + ciclo
                        + "; con este curso llegaria a " + total + " h (tope " + TOPE_HORAS_DOCENTE + " h).");
            }
        }

        if (!errores.isEmpty()) return ResponseEntity.badRequest().body(respuesta(false, errores, null));

        Map<String, Object> nueva = asignacion(ciclo, carrera, semestre, codigo, asignatura, grupo,
                ht, hp, docente, dpto, turno);
        asignaciones.add(nueva);
        String msg = "Curso " + codigo + " asignado a " + docente + ". Total del docente en "
                + ciclo + ": " + horasDocente(ciclo, docente) + " h.";
        return ResponseEntity.status(201).body(respuesta(true, List.of(msg), nueva));
    }

    private int horasDocente(String ciclo, String docente) {
        return asignaciones.stream()
                .filter(a -> a.get("ciclo").equals(ciclo) && a.get("docente").toString().equalsIgnoreCase(docente))
                .mapToInt(a -> (int) a.get("totalHoras"))
                .sum();
    }

    private Map<String, Object> asignacion(String ciclo, String carrera, int semestre, String codigo,
                                           String asignatura, String grupo, int ht, int hp,
                                           String docente, String dpto, String turno) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("id", secuencia.incrementAndGet());
        a.put("ciclo", ciclo);
        a.put("carrera", carrera);
        a.put("semestre", semestre);
        a.put("codigoCurso", codigo);
        a.put("asignatura", asignatura);
        a.put("grupo", grupo);
        a.put("horasTeoria", ht);
        a.put("horasPractica", hp);
        a.put("totalHoras", ht + hp);
        a.put("docente", docente);
        a.put("departamento", dpto);
        a.put("turno", turno);
        a.put("fechaRegistro", ahora());
        return a;
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

    private static Integer entero(Object v) {
        if (v instanceof Number n) {
            return n.doubleValue() == Math.floor(n.doubleValue()) ? n.intValue() : null;
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String ahora() {
        return LocalDateTime.now(ZoneId.of("America/Lima"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
