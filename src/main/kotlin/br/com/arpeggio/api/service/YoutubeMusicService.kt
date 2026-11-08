package br.com.arpeggio.api.service

import br.com.arpeggio.api.client.api.YoutubeClient
import br.com.arpeggio.api.client.dto.youtube.YoutubeArtistDto
import br.com.arpeggio.api.client.dto.youtube.YoutubeItemDto
import br.com.arpeggio.api.dto.request.RequestParams
import br.com.arpeggio.api.dto.request.RequestTipo
import br.com.arpeggio.api.dto.response.ItemResponse
import br.com.arpeggio.api.infra.exception.ArtistaNaoEncontradoException
import br.com.arpeggio.api.infra.exception.FalhaAoBuscarArtistasException
import br.com.arpeggio.api.infra.log.Logs
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service

@Service
class YoutubeMusicService(
    override val NOME_STREAMING: String = "Youtube Music",
    private val youtubeClient: YoutubeClient,
    private val objectMapper: ObjectMapper = ObjectMapper(),
) : CommandStreamingMusic, CommandStreamingPodcast {

    override suspend fun buscaPorArtista(requestParams: RequestParams): ItemResponse {
        Logs.info("ENTRY: YoutubeMusicService/buscaPorArtista", requestParams.id)
        return try {
            val artista = buscaArtista(requestParams)
            val totalLancamentos = buscaLancamentosDoArtista(requestParams, artista.id)

            ItemResponse(
                streaming = NOME_STREAMING,
                consulta = artista.name,
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

    override suspend fun buscaPorPodcast(requestParams: RequestParams): ItemResponse {
        return ItemResponse(
            streaming = NOME_STREAMING,
            consulta = requestParams.busca,
            erro = "busca por podcast ainda não implementada",
        )
    }

    private suspend fun buscaArtista(requestParams: RequestParams): YoutubeArtistDto {
        return try {
            val responseJson = youtubeClient.buscaArtistas(
                termo = requestParams.busca,
                regiao = requestParams.regiao.name,
                idRequisicao = requestParams.id,
            )
            val rootNode = objectMapper.readTree(responseJson)
            extrairArtistaDoJson(rootNode)
        } catch (ex: Exception) {
            if (ex is ArtistaNaoEncontradoException) throw ex
            throw FalhaAoBuscarArtistasException()
        }
    }

    private suspend fun buscaLancamentosDoArtista(requestParams: RequestParams, idArtista: String): Int {
        val lancamentosAcumulados = mutableListOf<YoutubeItemDto>()

        val querAlbum = requestParams.tipos.contains(RequestTipo.ALBUM)
        val querSingle = requestParams.tipos.contains(RequestTipo.SINGLE)
        val querEp = requestParams.tipos.contains(RequestTipo.EP)

        if (querAlbum) {
            val albuns = buscaLancamentosPorTipo(
                idArtista = idArtista,
                discographyParam = YoutubeClient.ALBUMS_DISCOGRAPHY_PARAM,
                isAlbum = true,
                regiao = requestParams.regiao.name,
                idRequisicao = requestParams.id,
            )
            lancamentosAcumulados.addAll(albuns)
        }

        if (querSingle || querEp) {
            val singlesEEps = buscaLancamentosPorTipo(
                idArtista = idArtista,
                discographyParam = YoutubeClient.SINGLES_EPS_DISCOGRAPHY_PARAM,
                isAlbum = false,
                regiao = requestParams.regiao.name,
                idRequisicao = requestParams.id,
            )

            val filtrados = singlesEEps.filter { item ->
                (querSingle && item.type.equals("SINGLE", ignoreCase = true)) ||
                (querEp && item.type.equals("EP", ignoreCase = true))
            }
            lancamentosAcumulados.addAll(filtrados)
        }

        val lancamentosSemDuplicatas = removerDuplicatas(lancamentosAcumulados)
        return lancamentosSemDuplicatas.size
    }

    private suspend fun buscaLancamentosPorTipo(
        idArtista: String,
        discographyParam: String,
        isAlbum: Boolean,
        regiao: String,
        idRequisicao: Int,
    ): List<YoutubeItemDto> {
        val responseDiscografia = runCatching {
            youtubeClient.buscaBrowse(
                browseId = "MPAD$idArtista",
                params = discographyParam,
                regiao = regiao,
                idRequisicao = idRequisicao,
            )
        }.getOrNull()

        if (responseDiscografia != null) {
            val rootNode = objectMapper.readTree(responseDiscografia)
            val lancamentosGrid = extrairLancamentosDoGrid(rootNode, isAlbum)
            if (lancamentosGrid.isNotEmpty()) {
                return lancamentosGrid
            }
        }

        // Fallback: se a discografia estendida não retornou itens, busca na página principal do canal do artista
        return runCatching {
            val responsePrincipal = youtubeClient.buscaBrowse(
                browseId = idArtista,
                params = null,
                regiao = regiao,
                idRequisicao = idRequisicao,
            )
            val rootNode = objectMapper.readTree(responsePrincipal)
            extrairLancamentosDoCarrossel(rootNode, isAlbum)
        }.getOrElse { emptyList() }
    }

    private fun extrairArtistaDoJson(rootNode: JsonNode): YoutubeArtistDto {
        val contents = rootNode
            .path("contents")
            .path("tabbedSearchResultsRenderer")
            .path("tabs")
            .path(0)
            .path("tabRenderer")
            .path("content")
            .path("sectionListRenderer")
            .path("contents")
            .path(0)
            .path("musicShelfRenderer")
            .path("contents")

        if (contents.isMissingNode || !contents.isArray || contents.isEmpty) {
            throw ArtistaNaoEncontradoException()
        }

        val primeiroItem = contents.path(0).path("musicResponsiveListItemRenderer")
        val nome = primeiroItem
            .path("flexColumns")
            .path(0)
            .path("musicResponsiveListItemFlexColumnRenderer")
            .path("text")
            .path("runs")
            .path(0)
            .path("text")
            .asText()

        val browseId = primeiroItem
            .path("navigationEndpoint")
            .path("browseEndpoint")
            .path("browseId")
            .asText()

        if (nome.isBlank() || browseId.isBlank()) {
            throw ArtistaNaoEncontradoException()
        }

        return YoutubeArtistDto(id = browseId, name = nome)
    }

    private fun extrairLancamentosDoGrid(rootNode: JsonNode, isAlbum: Boolean): List<YoutubeItemDto> {
        val items = rootNode
            .path("contents")
            .path("singleColumnBrowseResultsRenderer")
            .path("tabs")
            .path(0)
            .path("tabRenderer")
            .path("content")
            .path("sectionListRenderer")
            .path("contents")
            .path(0)
            .path("gridRenderer")
            .path("items")

        if (items.isMissingNode || !items.isArray) return emptyList()

        return items.mapNotNull { itemNode ->
            val renderer = itemNode.path("musicTwoRowItemRenderer")
            if (renderer.isMissingNode) return@mapNotNull null

            val titulo = renderer.path("title").path("runs").path(0).path("text").asText()
            val id = renderer.path("navigationEndpoint").path("browseEndpoint").path("browseId").asText()

            val subtitleRuns = renderer.path("subtitle").path("runs")
            val subtitleTexts = if (subtitleRuns.isArray) {
                subtitleRuns.map { it.path("text").asText() }
            } else {
                emptyList()
            }

            val tipoNormalizado = classificarTipo(subtitleTexts, isAlbum)
            val ano = subtitleTexts.firstOrNull { it.trim().matches(Regex("\\d{4}")) }?.trim()

            if (titulo.isNotBlank()) {
                YoutubeItemDto(id = id, title = titulo, type = tipoNormalizado, year = ano)
            } else {
                null
            }
        }
    }

    private fun extrairLancamentosDoCarrossel(rootNode: JsonNode, isAlbum: Boolean): List<YoutubeItemDto> {
        val shelves = rootNode
            .path("contents")
            .path("singleColumnBrowseResultsRenderer")
            .path("tabs")
            .path(0)
            .path("tabRenderer")
            .path("content")
            .path("sectionListRenderer")
            .path("contents")

        if (shelves.isMissingNode || !shelves.isArray) return emptyList()

        val tituloEsperado = if (isAlbum) "Álbuns" else "Singles e EPs"

        for (shelf in shelves) {
            val carousel = shelf.path("musicCarouselShelfRenderer")
            val headerTitle = carousel
                .path("header")
                .path("musicCarouselShelfBasicHeaderRenderer")
                .path("title")
                .path("runs")
                .path(0)
                .path("text")
                .asText()

            if (headerTitle.equals(tituloEsperado, ignoreCase = true) ||
                (isAlbum && headerTitle.contains("Álbum", ignoreCase = true)) ||
                (!isAlbum && (headerTitle.contains("Single", ignoreCase = true) || headerTitle.contains("EP", ignoreCase = true)))
            ) {
                val contents = carousel.path("contents")
                if (contents.isArray) {
                    return contents.mapNotNull { itemNode ->
                        val renderer = itemNode.path("musicTwoRowItemRenderer")
                        if (renderer.isMissingNode) return@mapNotNull null

                        val titulo = renderer.path("title").path("runs").path(0).path("text").asText()
                        val id = renderer.path("navigationEndpoint").path("browseEndpoint").path("browseId").asText()
                        val subtitleRuns = renderer.path("subtitle").path("runs")
                        val subtitleTexts = if (subtitleRuns.isArray) {
                            subtitleRuns.map { it.path("text").asText() }
                        } else {
                            emptyList()
                        }
                        val tipoNormalizado = classificarTipo(subtitleTexts, isAlbum)
                        val ano = subtitleTexts.firstOrNull { it.trim().matches(Regex("\\d{4}")) }?.trim()

                        if (titulo.isNotBlank()) {
                            YoutubeItemDto(id = id, title = titulo, type = tipoNormalizado, year = ano)
                        } else null
                    }
                }
            }
        }
        return emptyList()
    }

    private fun classificarTipo(subtitleTexts: List<String>, isAlbum: Boolean): String {
        val textsUpper = subtitleTexts.map { it.uppercase().trim() }
        return when {
            textsUpper.any { it == "EP" || (it.contains("EP") && !it.contains("EPISÓDIO")) } -> "EP"
            textsUpper.any { it.contains("SINGLE") } -> "SINGLE"
            textsUpper.any { it.contains("ÁLBUM") || it.contains("ALBUM") } -> "ALBUM"
            isAlbum -> "ALBUM"
            else -> "SINGLE"
        }
    }

    private fun removerDuplicatas(lancamentos: List<YoutubeItemDto>): List<YoutubeItemDto> {
        return lancamentos.distinctBy { lancamento ->
            gerarChaveDeduplicacao(lancamento)
        }
    }

    private fun gerarChaveDeduplicacao(lancamento: YoutubeItemDto): String {
        if (lancamento.title.isBlank()) return lancamento.id
        val tituloNormalizado = lancamento.title
            .replace("’", "'")
            .replace("…", "...")
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()
        return "${lancamento.type.uppercase()}_$tituloNormalizado"
    }
}