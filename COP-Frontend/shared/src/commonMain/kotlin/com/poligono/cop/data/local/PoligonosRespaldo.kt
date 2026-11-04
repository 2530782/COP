package com.poligono.cop.data.local

import com.poligono.cop.data.dto.PoligonoDto

/**
 * Copia de los polígonos dentro de la app para que el mapa funcione aunque
 * no haya internet la primera vez. Los datos oficiales vienen de GET /poligonos.
 */
object PoligonosRespaldo {

    val lista = listOf(
        PoligonoDto(
            id = 1,
            nombre = "Polígono Guadalupe",
            direccion = "C. Tolteca 407, Guadalupe 3ra Secc., 43650 Tulancingo, Hgo.",
            latitud = 20.0851588,
            longitud = -98.3549547,
            horario = "Lunes a viernes (confirmar horario con el Centro)",
            telefono = "775 755 8450 ext. 1135",
            servicios = "Fisioterapia, nutrición, psicología, talleres y cursos",
            esPrincipal = true,
            ubicacionAproximada = false
        ),
        PoligonoDto(
            id = 2,
            nombre = "Polígono La Cañada",
            direccion = "Níquel 134, Lomas del Progreso, 43615 Tulancingo, Hgo.",
            latitud = 20.1089023,
            longitud = -98.3558105,
            horario = "Lunes a viernes, 8:30 a 16:00",
            telefono = "775 688 0194",
            servicios = "Talleres, cursos y comedor",
            esPrincipal = false,
            ubicacionAproximada = false
        ),
        PoligonoDto(
            id = 3,
            nombre = "Polígono Jaltepec",
            direccion = "Centro, 43700 Jaltepec, Tulancingo, Hgo.",
            latitud = 20.1166436,
            longitud = -98.4173375,
            horario = "Consultar horario con el Centro",
            telefono = "775 755 8450 ext. 1135",
            servicios = "Talleres y cursos",
            esPrincipal = false,
            ubicacionAproximada = false
        ),
        PoligonoDto(
            id = 4,
            nombre = "Polígono Metilatla",
            direccion = "Col. Metilatla, 43633 Tulancingo, Hgo. (ubicación aproximada)",
            latitud = 20.091007,
            longitud = -98.3423853,
            horario = "Consultar horario con el Centro",
            telefono = "775 755 8450 ext. 1135",
            servicios = "Talleres y cursos",
            esPrincipal = false,
            ubicacionAproximada = true
        )
    )

    val principal: PoligonoDto
        get() = lista.first { it.esPrincipal }
}
