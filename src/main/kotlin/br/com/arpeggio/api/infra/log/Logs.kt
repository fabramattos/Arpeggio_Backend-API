package br.com.arpeggio.api.infra.log

import br.com.arpeggio.api.dto.request.RequestParams
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.WebClientResponseException

object Logs {

    private val logger: Logger = LoggerFactory.getLogger("Logs")

    fun error(nomeStreaming: String, idRequest: Int, msg: String, ex: Throwable? = null) {
        val detalhe = formatarDetalhesExcecao(ex)
        logger.error("ERRO EXTERNO: $nomeStreaming | requestId: $idRequest | msg: $msg$detalhe")
    }

    fun searchStarted(request: RequestParams) {
        logger.info("INFO: Pesquisando: $request")
    }

    fun searchCompleted(request: RequestParams) {
        logger.info("INFO: Finalizado: $request")
    }

    fun authenticationWarn(nomeStreaming: String) {
        logger.warn("WARN: '$nomeStreaming' NAO AUTENTICADO")
    }

    fun authenticated(nomeStreaming: String) {
        logger.info("INFO: '$nomeStreaming' : Autenticação realizada")
    }

    fun debug(message: String) {
        logger.debug("DEBUG: $message")
    }

    fun info(message: String, requestId: Int) {
        logger.info("INFO: $message | requestId: $requestId")
    }

    fun warn(nomeStreaming: String, idRequest: Int, msg: String, ex: Throwable? = null) {
        val detalhe = formatarDetalhesExcecao(ex)
        logger.warn("WARN: $nomeStreaming | msg: $msg$detalhe | requestId: $idRequest")
    }

    private fun formatarDetalhesExcecao(ex: Throwable?): String {
        if (ex == null) return ""
        return if (ex is WebClientResponseException) {
            val body = ex.responseBodyAsString.takeIf { it.isNotBlank() } ?: "(corpo vazio)"
            " | HttpStatusCode: ${ex.statusCode.value()} ${ex.statusText} | URL: ${ex.request?.uri} | ResponseBody: $body"
        } else {
            " | Causa: ${ex.javaClass.simpleName} - ${ex.message}"
        }
    }
}