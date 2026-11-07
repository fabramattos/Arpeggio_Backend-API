package br.com.arpeggio.api.service

import br.com.arpeggio.api.client.api.SpotifyClient
import br.com.arpeggio.api.dto.request.RequestParams
import br.com.arpeggio.api.dto.response.ItemResponse
import br.com.arpeggio.api.infra.log.Logs
import org.springframework.stereotype.Service

@Service
class SpotifyPodcastService(
    override val NOME_STREAMING: String = "Spotify",
    private val spotifyClient: SpotifyClient,
) : CommandStreamingPodcast {

    override suspend fun buscaPorPodcast(requestParams: RequestParams): ItemResponse {
        Logs.info("ENTRY: SpotifyPodcastService/buscaPorPodcast", requestParams.id)
        return try {
            val podcast = spotifyClient.buscaPodcasts(
                termo = requestParams.busca,
                regiao = requestParams.regiao.name,
                idRequisicao = requestParams.id,
            ).shows.items.first()

            ItemResponse(
                streaming = NOME_STREAMING,
                consulta = podcast.name,
                episodios = podcast.total_episodes
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
