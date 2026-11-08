package br.com.arpeggio.api.service

import br.com.arpeggio.api.client.api.AppleMusicClient
import br.com.arpeggio.api.dto.request.RequestParams
import br.com.arpeggio.api.dto.response.ItemResponse
import br.com.arpeggio.api.infra.exception.PodcastNaoEncontradoException
import br.com.arpeggio.api.infra.log.Logs
import org.springframework.stereotype.Service

@Service
class ApplePodcastService(
    override val NOME_STREAMING: String = "Apple Podcasts",
    private val appleMusicClient: AppleMusicClient,
) : CommandStreamingPodcast {

    override suspend fun buscaPorPodcast(requestParams: RequestParams): ItemResponse {
        Logs.info("ENTRY: ApplePodcastService/buscaPorPodcast", requestParams.id)
        return try {
            val response = appleMusicClient.buscaPodcasts(
                termo = requestParams.busca,
                regiao = requestParams.regiao.name,
                idRequisicao = requestParams.id,
            )

            val podcast = response.results.firstOrNull { it.kind == "podcast" || it.wrapperType == "track" }
                ?: throw PodcastNaoEncontradoException()

            val nomePodcast = podcast.collectionName ?: podcast.trackName ?: requestParams.busca
            val totalEpisodios = podcast.trackCount ?: 0

            ItemResponse(
                streaming = NOME_STREAMING,
                consulta = nomePodcast,
                episodios = totalEpisodios,
            )
        } catch (ex: Exception) {
            ItemResponse(
                streaming = NOME_STREAMING,
                consulta = requestParams.busca,
                erro = ex.message,
            )
        }
    }
}
