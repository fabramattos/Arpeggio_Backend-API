package br.com.arpeggio.api.service

import br.com.arpeggio.api.dto.externalApi.spotify.*
import br.com.arpeggio.api.dto.request.RequestParams
import br.com.arpeggio.api.dto.response.ItemResponse
import br.com.arpeggio.api.infra.exception.FalhaAoBuscarPodcastsException
import br.com.arpeggio.api.infra.log.Logs
import kotlinx.coroutines.delay
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import kotlin.time.Duration.Companion.milliseconds

@Service
class SpotifyPodcastService(
    override val NOME_STREAMING: String = "Spotify",
    private val authentication: SpotifyAuthentication,
    private val webClient: WebClient,
) : CommandStreamingPodcast {

    suspend fun buscaPodcasts(requestParams: RequestParams): SpotifyApiPodcastData {
        repeat(2) {
            try {
                Logs.info("ENTRY: SpotifyPodcastService/buscaPodcasts", requestParams.id)
                val uri = UriComponentsBuilder
                    .fromUri(URI("https://api.spotify.com/v1/search"))
                    .queryParam("q", requestParams.busca)
                    .queryParam("type", "show")
                    .queryParam("market", requestParams.regiao.name)
                    .queryParam("limit", 3)
                    .buildAndExpand()
                    .toUri()

                return webClient
                    .get()
                    .uri { uri }
                    .header("Authorization", authentication.headerValue)
                    .retrieve()
                    .awaitBody<SpotifyApiPodcastsResponse>()
                    .shows.items
                    .first()

            } catch (ex: Exception) {
                if (ex.localizedMessage?.contains("401") == true) {
                    Logs.authenticationWarn(NOME_STREAMING)
                    authentication.atualizaToken()
                } else {
                    Logs.error(NOME_STREAMING, requestParams.id, ex.localizedMessage ?: "Erro na busca")
                    delay(500.milliseconds)
                }
            }
        }
        throw FalhaAoBuscarPodcastsException()
    }


    override suspend fun buscaPorPodcast(requestParams: RequestParams): ItemResponse {
        Logs.info("ENTRY: SpotifyPodcastService/buscaPorPodcast", requestParams.id)
        try {
            val podcast = buscaPodcasts(requestParams)
            return ItemResponse(
                streaming = NOME_STREAMING,
                consulta = podcast.name,
                episodios = podcast.total_episodes
            )

        } catch (ex: Exception) {
            return ItemResponse(
                streaming = NOME_STREAMING,
                consulta = requestParams.busca,
                erro = ex.message
            )
        }
    }

}
