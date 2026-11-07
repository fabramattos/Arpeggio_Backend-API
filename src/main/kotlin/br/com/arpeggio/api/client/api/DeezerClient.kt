package br.com.arpeggio.api.client.api

import br.com.arpeggio.api.client.dto.deezer.DeezerApiAlbumsResponse
import br.com.arpeggio.api.client.dto.deezer.DeezerApiArtistsResponse
import br.com.arpeggio.api.client.dto.deezer.DeezerApiPodcastsResponse
import br.com.arpeggio.api.infra.exception.FalhaAoBuscarAlbunsDoArtista
import br.com.arpeggio.api.infra.exception.FalhaAoBuscarArtistasException
import br.com.arpeggio.api.infra.exception.FalhaAoBuscarDetalhesPodcastException
import br.com.arpeggio.api.infra.exception.FalhaAoBuscarPodcastsException
import br.com.arpeggio.api.infra.log.Logs
import kotlinx.coroutines.delay
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import kotlin.time.Duration.Companion.milliseconds

@Component
class DeezerClient(
    private val webClient: WebClient,
) {
    private val NOME_STREAMING = "Deezer"

    suspend fun buscaArtistas(termo: String, idRequisicao: Int = 0): DeezerApiArtistsResponse {
        repeat(2) {
            try {
                Logs.info("ENTRY: DeezerClient/buscaArtistas", idRequisicao)
                val uri = UriComponentsBuilder
                    .fromUri(URI("https://api.deezer.com/search/artist"))
                    .queryParam("q", termo)
                    .buildAndExpand()
                    .toUri()

                return webClient
                    .get()
                    .uri { uri }
                    .retrieve()
                    .awaitBody<DeezerApiArtistsResponse>()
            } catch (ex: Exception) {
                Logs.warn(NOME_STREAMING, idRequisicao, ex.localizedMessage ?: "Erro na busca de artista", ex)
                delay(500.milliseconds)
            }
        }
        throw FalhaAoBuscarArtistasException()
    }

    suspend fun buscaAlbuns(
        idArtista: Int,
        limite: Int = 999,
        idRequisicao: Int = 0,
    ): DeezerApiAlbumsResponse {
        repeat(2) {
            try {
                Logs.info("ENTRY: DeezerClient/buscaAlbuns", idRequisicao)
                val uri = UriComponentsBuilder
                    .fromUri(URI("https://api.deezer.com/artist/$idArtista/albums"))
                    .queryParam("limit", limite)
                    .buildAndExpand()
                    .toUri()

                return webClient
                    .get()
                    .uri { uri }
                    .retrieve()
                    .awaitBody<DeezerApiAlbumsResponse>()
            } catch (ex: Exception) {
                Logs.error(NOME_STREAMING, idRequisicao, ex.localizedMessage ?: "Erro na busca de albuns", ex)
                delay(500.milliseconds)
            }
        }
        throw FalhaAoBuscarAlbunsDoArtista()
    }

    suspend fun buscaPodcasts(termo: String, idRequisicao: Int = 0): DeezerApiPodcastsResponse {
        repeat(2) {
            try {
                Logs.info("ENTRY: DeezerClient/buscaPodcasts", idRequisicao)
                val uri = UriComponentsBuilder
                    .fromUri(URI("https://api.deezer.com/search/podcast"))
                    .queryParam("q", termo)
                    .buildAndExpand()
                    .toUri()

                return webClient
                    .get()
                    .uri { uri }
                    .retrieve()
                    .awaitBody<DeezerApiPodcastsResponse>()
            } catch (ex: Exception) {
                Logs.warn(NOME_STREAMING, idRequisicao, ex.localizedMessage ?: "Erro na busca de podcasts", ex)
                delay(500.milliseconds)
            }
        }
        throw FalhaAoBuscarPodcastsException()
    }

    suspend fun buscaEpisodiosPodcast(idPodcast: Int, idRequisicao: Int = 0): DeezerApiAlbumsResponse {
        repeat(2) {
            try {
                Logs.info("ENTRY: DeezerClient/buscaEpisodiosPodcast", idRequisicao)
                val uri = UriComponentsBuilder
                    .fromUri(URI("https://api.deezer.com/podcast/$idPodcast/episodes"))
                    .buildAndExpand()
                    .toUri()

                return webClient
                    .get()
                    .uri { uri }
                    .retrieve()
                    .awaitBody<DeezerApiAlbumsResponse>()
            } catch (ex: Exception) {
                Logs.error(NOME_STREAMING, idRequisicao, ex.localizedMessage ?: "Erro na busca de episodios do podcast", ex)
                delay(500.milliseconds)
            }
        }
        throw FalhaAoBuscarDetalhesPodcastException()
    }
}
