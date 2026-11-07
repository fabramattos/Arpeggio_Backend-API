package br.com.arpeggio.api.infra.configuration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.info.BuildProperties
import org.springframework.stereotype.Component

@Component
class ApiHealthIndicator(
    @param:Autowired(required = false)
    private val buildProperties: BuildProperties? = null
) : HealthIndicator {

    override fun health(): Health {
        val versao = buildProperties?.version ?: "dev"
        return Health.up()
            .withDetail("disponivel", true)
            .withDetail("versao", versao)
            .withDetail("descricao", "API Arpeggio operacional")
            .build()
    }
}
