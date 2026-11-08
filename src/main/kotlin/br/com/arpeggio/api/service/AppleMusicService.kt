package br.com.arpeggio.api.service

import br.com.arpeggio.api.client.api.AppleMusicClient
import br.com.arpeggio.api.client.dto.apple.AppleMusicItemDto
import br.com.arpeggio.api.client.dto.apple.AppleMusicResultItem
import br.com.arpeggio.api.dto.request.RequestParams
import br.com.arpeggio.api.dto.response.ItemResponse
import br.com.arpeggio.api.infra.exception.ArtistaNaoEncontradoException
import br.com.arpeggio.api.infra.log.Logs
import org.springframework.stereotype.Service

@Service
class AppleMusicService(
    override val NOME_STREAMING: String = "Apple Music",
    private val appleMusicClient: AppleMusicClient,
) : CommandStreamingMusic {

    override suspend fun buscaPorArtista(requestParams: RequestParams): ItemResponse {
        Logs.info("ENTRY: AppleMusicService/buscaPorArtista", requestParams.id)
        return try {
            val responseArtistas = appleMusicClient.buscaArtistas(
                termo = requestParams.busca,
                regiao = requestParams.regiao.name,
                idRequisicao = requestParams.id,
            )

            val artista = responseArtistas.results.firstOrNull { it.wrapperType == "artist" }
                ?: throw ArtistaNaoEncontradoException()

            val artistId = artista.artistId ?: throw ArtistaNaoEncontradoException()
            val artistName = artista.artistName ?: requestParams.busca

            val totalLancamentos = buscaLancamentosDoArtista(requestParams, artistId)

            ItemResponse(
                streaming = NOME_STREAMING,
                consulta = artistName,
                albuns = totalLancamentos,
            )
        } catch (ex: Exception) {
            ItemResponse(
                streaming = NOME_STREAMING,
                consulta = requestParams.busca,
                erro = ex.message,
            )
        }
    }

    private suspend fun buscaLancamentosDoArtista(requestParams: RequestParams, idArtista: Long): Int {
        val responseAlbuns = appleMusicClient.buscaAlbunsDoArtista(
            idArtista = idArtista,
            regiao = requestParams.regiao.name,
            limite = 200,
            idRequisicao = requestParams.id,
        )

        val colecoes = responseAlbuns.results.filter { it.wrapperType == "collection" }

        val lancamentos = colecoes.mapNotNull { item ->
            val tipo = classificarTipo(item)
            val ehTipoDesejado = requestParams.tipos.any { it.name.equals(tipo, ignoreCase = true) }
            if (ehTipoDesejado) {
                AppleMusicItemDto(
                    id = (item.collectionId ?: 0).toString(),
                    title = item.collectionName.orEmpty(),
                    type = tipo,
                    year = item.releaseDate?.take(4),
                )
            } else {
                null
            }
        }

        val lancamentosSemDuplicatas = removerDuplicatas(lancamentos)
        return lancamentosSemDuplicatas.size
    }

    private fun classificarTipo(item: AppleMusicResultItem): String {
        val name = item.collectionName.orEmpty().trim()
        val trackCount = item.trackCount ?: 0

        return when {
            name.endsWith(" - Single", ignoreCase = true) ||
            name.endsWith(" (Single)", ignoreCase = true) ||
            trackCount <= 1 -> "SINGLE"

            name.endsWith(" - EP", ignoreCase = true) ||
            name.endsWith(" (EP)", ignoreCase = true) ||
            (trackCount in 2..6 && name.contains("EP", ignoreCase = true)) -> "EP"

            else -> "ALBUM"
        }
    }

    private fun removerDuplicatas(lancamentos: List<AppleMusicItemDto>): List<AppleMusicItemDto> {
        return lancamentos.distinctBy { lancamento ->
            gerarChaveDeduplicacao(lancamento)
        }
    }

    private fun gerarChaveDeduplicacao(lancamento: AppleMusicItemDto): String {
        if (lancamento.title.isBlank()) return lancamento.id
        val tituloNormalizado = lancamento.title
            .replace("’", "'")
            .replace("…", "...")
            .replace(Regex("(?i)\\s*-\\s*(single|ep)$"), "")
            .replace(Regex("(?i)\\s*\\((single|ep)\\)$"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()
        return "${lancamento.type.uppercase()}_$tituloNormalizado"
    }
}
