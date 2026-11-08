package br.com.arpeggio.api.client.api

import br.com.arpeggio.api.client.dto.apple.AppleMusicSearchResponse
import br.com.arpeggio.api.infra.exception.FalhaAoBuscarAlbunsDoArtista
import br.com.arpeggio.api.infra.exception.FalhaAoBuscarArtistasException
import br.com.arpeggio.api.infra.log.Logs
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.delay
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import kotlin.time.Duration.Companion.milliseconds

@Component
class AppleMusicClient(
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper = ObjectMapper(),
) {
    private val NOME_STREAMING = "Apple Music"

    suspend fun buscaArtistas(
        termo: String,
        regiao: String = "BR",
        idRequisicao: Int = 0,
    ): AppleMusicSearchResponse {
        val uri = UriComponentsBuilder
            .fromUri(URI("https://itunes.apple.com/search"))
            .queryParam("term", termo)
            .queryParam("entity", "musicArtist")
            .queryParam("country", regiao)
            .queryParam("limit", 1)
            .buildAndExpand()
            .encode()
            .toUri()

        repeat(2) {
            try {
                Logs.info("ENTRY: AppleMusicClient/buscaArtistas", idRequisicao)
                val responseJson = webClient
                    .get()
                    .uri { uri }
                    .retrieve()
                    .awaitBody<String>()

                return objectMapper.readValue(responseJson, AppleMusicSearchResponse::class.java)
            } catch (ex: Exception) {
                Logs.warn(NOME_STREAMING, idRequisicao, ex.localizedMessage ?: "Erro na busca de artista", ex)
                delay(500.milliseconds)
            }
        }
        throw FalhaAoBuscarArtistasException()
    }

    suspend fun buscaAlbunsDoArtista(
        idArtista: Long,
        regiao: String = "BR",
        limite: Int = 200,
        idRequisicao: Int = 0,
    ): AppleMusicSearchResponse {
        val uri = UriComponentsBuilder
            .fromUri(URI("https://itunes.apple.com/lookup"))
            .queryParam("id", idArtista)
            .queryParam("entity", "album")
            .queryParam("country", regiao)
            .queryParam("limit", limite)
            .buildAndExpand()
            .encode()
            .toUri()

        repeat(2) {
            try {
                Logs.info("ENTRY: AppleMusicClient/buscaAlbunsDoArtista", idRequisicao)
                val responseJson = webClient
                    .get()
                    .uri { uri }
                    .retrieve()
                    .awaitBody<String>()

                return objectMapper.readValue(responseJson, AppleMusicSearchResponse::class.java)
            } catch (ex: Exception) {
                Logs.warn(NOME_STREAMING, idRequisicao, ex.localizedMessage ?: "Erro na busca de álbuns", ex)
                delay(500.milliseconds)
            }
        }
        throw FalhaAoBuscarAlbunsDoArtista()
    }
}
