package br.com.arpeggio.api.client.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig {

    @Bean
    fun webClient(): WebClient {
        val size = 16 * 1024 * 1024 // 16 MB para suportar payloads grandes como InnerTube
        val strategies = ExchangeStrategies.builder()
            .codecs { codecs -> codecs.defaultCodecs().maxInMemorySize(size) }
            .build()

        return WebClient.builder()
            .exchangeStrategies(strategies)
            .defaultHeader(
                HttpHeaders.USER_AGENT,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
            )
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE, "*/*")
            .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7")
            .build()
    }
}
