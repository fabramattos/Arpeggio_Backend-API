package br.com.arpeggio.api.service

import br.com.arpeggio.api.dto.externalApi.deezer.DeezerApiAlbumsResponse
import br.com.arpeggio.api.dto.externalApi.deezer.DeezerApiPodcastData
import br.com.arpeggio.api.dto.externalApi.deezer.DeezerApiPodcastsResponse
import br.com.arpeggio.api.dto.request.RequestParams
import br.com.arpeggio.api.dto.response.ItemResponse
import br.com.arpeggio.api.infra.exception.FalhaAoBuscarDetalhesPodcastException
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
class DeezerPodcastService(
    override val NOME_STREAMING: String = "Deezer",
    val webClient: WebClient
) : CommandStreamingPodcast {

    private suspend fun buscaPodcasts(requestParams: RequestParams): DeezerApiPodcastData {
        repeat(2) {
            try {
                Logs.info("ENTRY: DeezerPodcastService/buscaPodcasts", requestParams.id)
                val uri = UriComponentsBuilder
                    .fromUri(URI("https://api.deezer.com/search/podcast"))
                    .queryParam("q", requestParams.busca)
                    .buildAndExpand()
                    .toUri()

                return webClient
                    .get()
                    .uri { uri }
                    .retrieve()
                    .awaitBody<DeezerApiPodcastsResponse>()
                    .data
                    .first()

            } catch (ex: Exception) {
                Logs.warn(NOME_STREAMING, requestParams.id, ex.localizedMessage ?: "Erro na busca de podcasts")
                delay(500.milliseconds)
            }
        }
        throw FalhaAoBuscarPodcastsException()
    }

    private suspend fun buscaEpisodiosDoPodcast(requestParam: RequestParams, idPodcast: Int): Int {
        repeat(2) {
            try {
                Logs.info("ENTRY: DeezerPodcastService/buscaEpisodiosDoPodcast", requestParam.id)
                val uri = UriComponentsBuilder
                    .fromUri(URI("https://api.deezer.com/podcast/$idPodcast/episodes"))
                    .buildAndExpand()
                    .toUri()

                return webClient
                    .get()
                    .uri { uri }
                    .retrieve()
                    .awaitBody<DeezerApiAlbumsResponse>()
                    .total
            } catch (ex: Exception) {
                Logs.error(NOME_STREAMING, requestParam.id, ex.localizedMessage ?: "Erro na busca de episodios do podcast")
                delay(500.milliseconds)
            }
        }
        throw FalhaAoBuscarDetalhesPodcastException()
    }

    override suspend fun buscaPorPodcast(requestParams: RequestParams): ItemResponse {
        Logs.info("ENTRY: DeezerPodcastService/buscaPorPodcast", requestParams.id)
        return try {
            val podcast = buscaPodcasts(requestParams)
            val totalDeEpisodios = buscaEpisodiosDoPodcast(requestParams, podcast.id)
            ItemResponse(
                streaming = NOME_STREAMING,
                consulta = podcast.title,
                episodios = totalDeEpisodios
            )
        } catch (ex: Exception) {
            ItemResponse(
                streaming = NOME_STREAMING,
                consulta = requestParams.busca,
                erro = ex.message
            )
        }
    }
}
