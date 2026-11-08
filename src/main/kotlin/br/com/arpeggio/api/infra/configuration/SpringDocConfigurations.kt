package br.com.arpeggio.api.infra.configuration

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SpringDocConfigurations {

    @Bean
    fun customOpenAPI(): OpenAPI = OpenAPI()
        .servers(
            listOf(
                Server().url("https://arpeggio.up.railway.app").description("Production Server (Railway)"),
                Server().url("http://localhost:8080").description("Local Development Server")
            )
        )
        .info(
            Info().apply {
                version = "1.0.00"
                title = "Arpeggio API"
                description = """
                    ### Overview
                    Aggregation and simultaneous comparison API across multiple audio streaming platforms.
                    
                    **Supported Platforms:**
                    - **Music:** Spotify, Deezer, Tidal, YouTube Music, and Apple Music.
                    - **Podcasts:** Spotify, Deezer, and Apple Podcasts.
                    
                    ### How to Use
                    - **Music Releases (`/v1/artista`):** Search for an artist or band providing country (`regiao`) and content types (`tipo`: `ALBUM`, `SINGLE`, `EP`). The response aggregates catalog counts from each streaming service individually.
                    - **Podcasts (`/v1/podcast`):** Search for a podcast by name and country (`regiao`) to check the exact number of published episodes via official RSS feeds.
                    
                    ### Machine-Readable API Contract
                    The full OpenAPI 3.0 specification in JSON format (ideal for frontend code generators and AI agents) is available at:
                    `/swagger.json`
                """.trimIndent()
                contact = Contact().apply {
                    name = "Felipe Mattos"
                }
            }
        )
}