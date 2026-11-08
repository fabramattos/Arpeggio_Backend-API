package br.com.arpeggio.api.client.api

import br.com.arpeggio.api.client.dto.youtube.YoutubeBrowseRequest
import br.com.arpeggio.api.client.dto.youtube.YoutubeClientInfo
import br.com.arpeggio.api.client.dto.youtube.YoutubeRequestContext
import br.com.arpeggio.api.client.dto.youtube.YoutubeSearchRequest
import br.com.arpeggio.api.infra.exception.FalhaAoBuscarAlbunsDoArtista
import br.com.arpeggio.api.infra.exception.FalhaAoBuscarArtistasException
import br.com.arpeggio.api.infra.log.Logs
import kotlinx.coroutines.delay
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import kotlin.time.Duration.Companion.milliseconds

@Component
class YoutubeClient(
    private val webClient: WebClient,
) {
    private val NOME_STREAMING = "Youtube Music"

    companion object {
        private const val BASE_URL = "https://music.youtube.com/youtubei/v1"
        const val ALBUMS_DISCOGRAPHY_PARAM = "ggMIegYIARoCAQI="
        const val SINGLES_EPS_DISCOGRAPHY_PARAM = "ggMIegYIAhoCAQI="
    }

    suspend fun buscaArtistas(
        termo: String,
        regiao: String = "BR",
        idRequisicao: Int = 0,
    ): String {
        val requestPayload = YoutubeSearchRequest(
            context = YoutubeRequestContext(client = YoutubeClientInfo(gl = regiao)),
            query = termo,
        )

        repeat(2) {
            try {
                Logs.info("ENTRY: YoutubeClient/buscaArtistas", idRequisicao)
                return webClient
                    .post()
                    .uri("$BASE_URL/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Origin", "https://music.youtube.com")
                    .bodyValue(requestPayload)
                    .retrieve()
                    .awaitBody<String>()
            } catch (ex: Exception) {
                Logs.warn(NOME_STREAMING, idRequisicao, ex.localizedMessage ?: "Erro na busca de artista", ex)
                delay(500.milliseconds)
            }
        }
        throw FalhaAoBuscarArtistasException()
    }

    suspend fun buscaBrowse(
        browseId: String,
        params: String? = null,
        regiao: String = "BR",
        idRequisicao: Int = 0,
    ): String {
        val requestPayload = YoutubeBrowseRequest(
            context = YoutubeRequestContext(client = YoutubeClientInfo(gl = regiao)),
            browseId = browseId,
            params = params,
        )

        repeat(2) {
            try {
                Logs.info("ENTRY: YoutubeClient/buscaBrowse", idRequisicao)
                return webClient
                    .post()
                    .uri("$BASE_URL/browse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Origin", "https://music.youtube.com")
                    .bodyValue(requestPayload)
                    .retrieve()
                    .awaitBody<String>()
            } catch (ex: Exception) {
                Logs.warn(NOME_STREAMING, idRequisicao, ex.localizedMessage ?: "Erro no browse do YouTube", ex)
                delay(500.milliseconds)
            }
        }
        throw FalhaAoBuscarAlbunsDoArtista()
    }
}
