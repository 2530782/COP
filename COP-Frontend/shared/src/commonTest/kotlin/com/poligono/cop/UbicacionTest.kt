package com.poligono.cop

import com.poligono.cop.domain.model.Poligono
import com.poligono.cop.domain.ubicacion.Coordenada
import com.poligono.cop.domain.ubicacion.Distancias
import com.poligono.cop.domain.ubicacion.FiltroUbicacion
import com.poligono.cop.domain.usecase.PoligonosCercanosUseCase
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pruebas del sensor GPS (calibración y procesamiento). Se corren con ./gradlew :shared:testAndroidHostTest */
class UbicacionTest {

    private fun poligono(id: Int, nombre: String, lat: Double, lon: Double) =
        Poligono(id, nombre, "", lat, lon, "", "", "", esPrincipal = id == 1, ubicacionAproximada = false)

    @Test
    fun distanciaEntreGuadalupeYLaCanada() {
        val metros = Distancias.metros(20.0851588, -98.3549547, 20.1089023, -98.3558105)
        assertTrue(metros in 2500.0..2800.0)
        assertEquals("2.6 km", Distancias.texto(metros))
        assertEquals("840 m", Distancias.texto(843.0))
    }

    @Test
    fun calibracionDescartaLecturasImprecisas() {
        val filtro = FiltroUbicacion()
        assertNull(filtro.procesar(Coordenada(20.0, -98.0, precisionMetros = 500f)))
    }

    @Test
    fun procesamientoPromediaLecturas() {
        val filtro = FiltroUbicacion()
        filtro.procesar(Coordenada(20.0, -98.0, precisionMetros = 10f))
        val promedio = filtro.procesar(Coordenada(20.001, -98.0, precisionMetros = 10f))!!
        assertTrue(abs(promedio.latitud - 20.0005) < 1e-9)
    }

    @Test
    fun ordenaPoligonosDelMasCercanoAlMasLejano() {
        val lista = listOf(
            poligono(1, "Guadalupe", 20.0851588, -98.3549547),
            poligono(3, "Jaltepec", 20.1166436, -98.4173375),
            poligono(2, "La Cañada", 20.1089023, -98.3558105)
        )

        // Persona parada junto a La Cañada
        val ordenados = PoligonosCercanosUseCase()(lista, Coordenada(20.1085, -98.3560))

        assertEquals("La Cañada", ordenados.first().poligono.nombre)
        assertEquals("Jaltepec", ordenados.last().poligono.nombre)
    }

    @Test
    fun sinGpsNoHayDistancias() {
        val ordenados = PoligonosCercanosUseCase()(listOf(poligono(1, "Guadalupe", 20.0, -98.0)), null)
        assertNull(ordenados.first().metros)
    }
}
