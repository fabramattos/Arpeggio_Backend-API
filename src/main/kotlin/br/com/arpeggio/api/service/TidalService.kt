package br.com.arpeggio.api.service

import br.com.arpeggio.api.client.api.TidalClient
import br.com.arpeggio.api.client.dto.tidal.TidalResult
import br.com.arpeggio.api.dto.request.RequestParams
import br.com.arpeggio.api.dto.request.RequestTipo
import br.com.arpeggio.api.dto.response.ItemResponse
import br.com.arpeggio.api.infra.exception.ArtistaNaoEncontradoException
import br.com.arpeggio.api.infra.exception.FalhaAoBuscarAlbunsDoArtista
import br.com.arpeggio.api.infra.exception.FalhaAoBuscarArtistasException
import br.com.arpeggio.api.infra.exception.FalhaInformacoesImprecisasDireitosAutoraisException
import br.com.arpeggio.api.infra.log.Logs
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.net.URI

private const val DELAY_REQUEST = 300L
private const val DELAY_RETRY = 1000L
private const val MAX_PAGINAS = 30
private const val VALIDADE_TOKEN = 86400 * 1000L

@Service
class TidalService(
    override val NOME_STREAMING: String = "Tidal",
    private val tidalClient: TidalClient,
) : CommandStreamingMusic, CommandStreamingPodcast {

    @Scheduled(fixedRate = VALIDADE_TOKEN)
    fun agendarAtualizacaoToken() {
        CoroutineScope(Dispatchers.Default).launch {
            tidalClient.atualizaToken()
        }
    }

    override suspend fun buscaPorArtista(requestParams: RequestParams): ItemResponse {
        Logs.info("ENTRY: TidalService/buscaPorArtista", requestParams.id)
        return try {
            val artista = buscaArtista(requestParams)
                .apply { qty = buscaAlbunsDoArtista(requestParams, id) }

            ItemResponse(
                streaming = NOME_STREAMING,
                consulta = artista.name,
                albuns = artista.qty
            )
        } catch (ex: Exception) {
            ItemResponse(
                streaming = NOME_STREAMING,
                consulta = requestParams.busca,
                erro = ex.message
            )
        }
    }

    override suspend fun buscaPorPodcast(requestParams: RequestParams): ItemResponse {
        return ItemResponse(
            streaming = NOME_STREAMING,
            consulta = requestParams.busca,
            erro = "busca por podcast ainda não implementada"
        )
    }

    private suspend fun buscaArtista(requestParams: RequestParams): TidalResult {
        return try {
            val response = tidalClient.buscaArtistas(
                termo = requestParams.busca,
                regiao = requestParams.regiao.name,
                idRequisicao = requestParams.id,
            )
            val rootNode = ObjectMapper().readTree(response)
            extrairArtistaDoJson(rootNode, requestParams.busca)
        } catch (ex: Exception) {
            if (ex is ArtistaNaoEncontradoException) throw ex
            throw FalhaAoBuscarArtistasException()
        }
    }

    private fun extrairArtistaDoJson(rootNode: JsonNode, buscaPadrao: String): TidalResult {
        val primeiroArtistaId = rootNode
            .path("data")
            .firstOrNull()
            ?.path("relationships")
            ?.path("artists")
            ?.path("data")
            ?.firstOrNull()
            ?.path("id")
            ?.asText()
            ?.takeIf { it.isNotBlank() }
            ?: throw ArtistaNaoEncontradoException()

        val artistaNome = rootNode
            .path("included")
            .firstOrNull { it.path("id").asText() == primeiroArtistaId }
            ?.path("attributes")
            ?.path("name")
            ?.asText()
            ?: buscaPadrao

        return TidalResult(primeiroArtistaId, artistaNome, 0)
    }

    private suspend fun buscaAlbunsDoArtista(requestParams: RequestParams, idArtista: String): Int {
        var errosReq = 0
        val lancamentosAcumulados = mutableListOf<LancamentoTidal>()
        var paginaAtual = 0
        var proximaUri: URI? = null
        var primeiraPagina = true

        while ((primeiraPagina || proximaUri != null) && paginaAtual < MAX_PAGINAS && errosReq <= 3) {
            val uriAtual = proximaUri
            val resultado = runCatching {
                val response = if (primeiraPagina) {
                    tidalClient.buscaAlbunsDoArtista(idArtista, requestParams.regiao.name, requestParams.id)
                } else {
                    tidalClient.buscaPorUri(uriAtual!!, requestParams.id)
                }
                primeiraPagina = false

                val rootNode = ObjectMapper().readTree(response)
                val lancamentosDaPagina = extrairLancamentosDaPagina(rootNode, requestParams)
                lancamentosAcumulados.addAll(lancamentosDaPagina)

                extrairProximaUri(rootNode)
            }

            resultado.onSuccess { nextUri ->
                proximaUri = nextUri
                paginaAtual++
                delay(DELAY_REQUEST)
            }

            resultado.onFailure {
                if (it is FalhaInformacoesImprecisasDireitosAutoraisException) throw it
                val is429 = it.localizedMessage?.contains("429") == true || it.cause?.localizedMessage?.contains("429") == true
                if (is429) {
                    errosReq++
                    delay(DELAY_RETRY)
                } else {
                    throw FalhaAoBuscarAlbunsDoArtista()
                }
            }
        }

        val lancamentosSemDuplicatas = removerDuplicatas(lancamentosAcumulados)
        return lancamentosSemDuplicatas.size
    }

    private fun removerDuplicatas(lancamentos: List<LancamentoTidal>): List<LancamentoTidal> {
        return lancamentos.distinctBy { lancamento ->
            gerarChaveDeduplicacao(lancamento)
        }
    }

    private fun gerarChaveDeduplicacao(lancamento: LancamentoTidal): String {
        if (lancamento.titulo.isBlank()) return lancamento.id
        val tituloNormalizado = lancamento.titulo
            .replace("’", "'")
            .replace("…", "...")
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()
        return "${lancamento.tipo.uppercase()}_$tituloNormalizado"
    }

    private fun extrairLancamentosDaPagina(
        rootNode: JsonNode,
        requestParams: RequestParams,
    ): List<LancamentoTidal> {
        val albunsNode = rootNode.path("included")
        return albunsNode.mapNotNull { albumNode ->
            validarRestricaoAutoral(albumNode, requestParams)

            val tipo = extrairTipoLancamento(albumNode)
            val titulo = albumNode.path("attributes").path("title").asText()
            val id = albumNode.path("id").asText()

            if (ehTipoPermitido(tipo, requestParams)) {
                LancamentoTidal(id, tipo, titulo)
            } else {
                null
            }
        }
    }

    private fun validarRestricaoAutoral(albumNode: JsonNode, requestParams: RequestParams) {
        val status = albumNode.path("status").asInt()
        if (status == 451 && !verificaSePodeIgnorarRestricaoAutoral(requestParams)) {
            throw FalhaInformacoesImprecisasDireitosAutoraisException()
        }
    }

    private fun extrairTipoLancamento(albumNode: JsonNode): String {
        return albumNode.path("attributes").path("type").asText()
            .ifBlank { albumNode.path("attributes").path("albumType").asText() }
            .ifBlank { albumNode.path("resource").path("type").asText() }
    }

    private fun ehTipoPermitido(tipo: String, requestParams: RequestParams): Boolean {
        return if (tipo.isBlank()) {
            verificaSePodeIgnorarRestricaoAutoral(requestParams)
        } else {
            requestParams.tipos.any { it.name.equals(tipo, ignoreCase = true) }
        }
    }

    private fun extrairProximaUri(rootNode: JsonNode): URI? {
        val rawNext = rootNode.path("links").path("next").asText().takeIf { it.isNotBlank() }
            ?: rootNode.path("data").path("relationships").path("albums").path("links").path("next").asText().takeIf { it.isNotBlank() }
            ?: return null

        val fullUrl = if (rawNext.startsWith("http")) rawNext else "https://openapi.tidal.com/v2$rawNext"
        val urlComInclude = if (!fullUrl.contains("include=albums")) {
            if (fullUrl.contains("?")) "$fullUrl&include=albums" else "$fullUrl?include=albums"
        } else {
            fullUrl
        }
        return URI(urlComInclude)
    }

    private fun verificaSePodeIgnorarRestricaoAutoral(requestParams: RequestParams): Boolean {
        return requestParams
            .tipos.containsAll(listOf(RequestTipo.ALBUM, RequestTipo.SINGLE))
    }
}

private data class LancamentoTidal(
    val id: String,
    val tipo: String,
    val titulo: String,
)