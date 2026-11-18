package ace.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Relógio da aplicação no fuso da academia. O container roda em UTC; sem isso
 * as datas de confirmação e de vencimento ficariam 3 horas adiantadas.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.timezone:America/Sao_Paulo}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }
}
