
package com.example.cargalectiva;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalogos")
public class CatalogosRestController {

    // INVOCACIÓN 1: Consultar catálogo de carreras
    @GetMapping("/carreras")
    public List<Map<String, Object>> obtenerCarreras() {

        return List.of(
            Map.of(
                "id", 1,
                "descripcion", "Ingeniería de Software",
                "codigo", "ISW",
                "facultad", "Facultad de Ingeniería",
                "estado", "Activa"
            ),
            Map.of(
                "id", 2,
                "descripcion", "Administración y Negocios Internacionales",
                "codigo", "ANI",
                "facultad", "Facultad de Ciencias Empresariales",
                "estado", "Activa"
            ),
            Map.of(
                "id", 3,
                "descripcion", "Derecho",
                "codigo", "DER",
                "facultad", "Facultad de Derecho",
                "estado", "Activa"
            ),
            Map.of(
                "id", 4,
                "descripcion", "Psicología",
                "codigo", "PSI",
                "facultad", "Facultad de Ciencias Humanas",
                "estado", "Activa"
            )
        );
    }

    // INVOCACIÓN 2: Consultar catálogo de planes de estudio
    @GetMapping("/planes-estudio")
    public List<Map<String, Object>> obtenerPlanesEstudio() {

        return List.of(
            Map.of(
                "id", 101,
                "descripcion", "Plan de Estudios 2022",
                "carreraId", 1,
                "carrera", "Ingeniería de Software",
                "version", "2022",
                "creditos", 200,
                "estado", "Vigente"
            ),
            Map.of(
                "id", 102,
                "descripcion", "Plan de Estudios 2024",
                "carreraId", 1,
                "carrera", "Ingeniería de Software",
                "version", "2024",
                "creditos", 210,
                "estado", "Vigente"
            ),
            Map.of(
                "id", 103,
                "descripcion", "Plan de Estudios 2023",
                "carreraId", 2,
                "carrera", "Administración y Negocios Internacionales",
                "version", "2023",
                "creditos", 200,
                "estado", "Vigente"
            ),
            Map.of(
                "id", 104,
                "descripcion", "Plan de Estudios 2024",
                "carreraId", 3,
                "carrera", "Derecho",
                "version", "2024",
                "creditos", 210,
                "estado", "Vigente"
            )
        );
    }
}
