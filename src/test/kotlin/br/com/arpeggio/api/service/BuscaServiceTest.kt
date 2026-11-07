package br.com.arpeggio.api.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.web.util.UriComponentsBuilder

class BuscaServiceTest {

    @Test
    fun `deve construir e codificar uri com espacos corretamente`() {
        val uri = UriComponentsBuilder
            .fromUriString("https://openapi.tidal.com/v2/searchResults")
            .queryParam("countryCode", "BR")
            .queryParam("filter[query]", "{query}")
            .queryParam("include", "artists")
            .buildAndExpand("dream theater")
            .encode()
            .toUri()

        println("URI GERADA TIDAL: $uri")
        println("ASCII STRING: ${uri.toASCIIString()}")
        assertEquals("https://openapi.tidal.com/v2/searchResults?countryCode=BR&filter%5Bquery%5D=dream%20theater&include=artists", uri.toASCIIString())
    }

    @Test
    fun `deve construir e codificar uri de albuns do artista no formato tidal v2`() {
        val uri = UriComponentsBuilder
            .fromUriString("https://openapi.tidal.com/v2/artists/{idArtista}")
            .queryParam("countryCode", "US")
            .queryParam("include", "albums")
            .buildAndExpand("8405")
            .encode()
            .toUri()

        assertEquals("https://openapi.tidal.com/v2/artists/8405?countryCode=US&include=albums", uri.toASCIIString())
    }
}