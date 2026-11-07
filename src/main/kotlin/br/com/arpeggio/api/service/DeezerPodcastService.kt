package br.com.arpeggio.api.service

import br.com.arpeggio.api.client.api.DeezerClient
import br.com.arpeggio.api.dto.request.RequestParams
import br.com.arpeggio.api.dto.response.ItemResponse
import br.com.arpeggio.api.infra.log.Logs
import org.springframework.stereotype.Service

@Service
class DeezerPodcastService(
    override val NOME_STREAMING: String = "Deezer",
    private val deezerClient: DeezerClient,
) : CommandStreamingPodcast {

    override suspend fun buscaPorPodcast(requestParams: RequestParams): ItemResponse {
        Logs.info("ENTRY: DeezerPodcastService/buscaPorPodcast", requestParams.id)
        return try {
            val podcast = deezerClient.buscaPodcasts(
                termo = requestParams.busca,
                idRequisicao = requestParams.id,
            ).data.first()

            val totalDeEpisodios = deezerClient.buscaEpisodiosPodcast(
                idPodcast = podcast.id,
                idRequisicao = requestParams.id,
            ).total

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
