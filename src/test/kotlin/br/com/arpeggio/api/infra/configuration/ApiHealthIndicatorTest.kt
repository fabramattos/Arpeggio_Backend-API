package br.com.arpeggio.api.infra.configuration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.boot.actuate.health.Status
import org.springframework.boot.info.BuildProperties
import java.util.*

class ApiHealthIndicatorTest {

    @Test
    fun `deve retornar status UP com versao dev quando buildProperties for nulo`() {
        val indicator = ApiHealthIndicator(buildProperties = null)
        val health = indicator.health()

        assertEquals(Status.UP, health.status)
        assertEquals(true, health.details["disponivel"])
        assertEquals("dev", health.details["versao"])
    }

    @Test
    fun `deve retornar versao do buildProperties quando disponivel`() {
        val properties = Properties().apply {
            setProperty("version", "0.1.02")
        }
        val buildProperties = BuildProperties(properties)
        val indicator = ApiHealthIndicator(buildProperties = buildProperties)
        val health = indicator.health()

        assertEquals(Status.UP, health.status)
        assertEquals(true, health.details["disponivel"])
        assertEquals("0.1.02", health.details["versao"])
    }
}
