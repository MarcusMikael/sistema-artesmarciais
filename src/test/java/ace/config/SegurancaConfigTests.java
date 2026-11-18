package ace.config;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SegurancaConfigTests {

    /** Relógio que dá para avançar no teste. */
    static final class RelogioAjustavel extends Clock {
        Instant agora = Instant.parse("2026-11-18T12:00:00Z");

        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
    }

    @Test
    void senhaDoAdminPrecisaSerForte() {
        assertThatThrownBy(() -> SecurityConfig.validarSenha("", false))
            .hasMessageContaining("ADMIN_PASSWORD");
        assertThatThrownBy(() -> SecurityConfig.validarSenha("alessandro123", false))
            .hasMessageContaining("publicada");
        assertThatThrownBy(() -> SecurityConfig.validarSenha("curta123", false))
            .hasMessageContaining("10 caracteres");
        assertThatCode(() -> SecurityConfig.validarSenha("uma-senha-bem-forte", false))
            .doesNotThrowAnyException();
        // Perfil de testes/desenvolvimento pode usar a senha de exemplo.
        assertThatCode(() -> SecurityConfig.validarSenha("alessandro123", true))
            .doesNotThrowAnyException();
        assertThatThrownBy(() -> SecurityConfig.validarSenha(" ", true))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void bloqueiaIpDepoisDeCincoFalhasEExpira() {
        RelogioAjustavel relogio = new RelogioAjustavel();
        LoginAttemptService s = new LoginAttemptService(relogio, 5, 20, 15, 15);

        for (int i = 0; i < 4; i++) {
            s.registrarFalha("1.1.1.1", "admin@x.com");
        }
        assertThat(s.bloqueio("1.1.1.1", "admin@x.com")).isZero();

        s.registrarFalha("1.1.1.1", "admin@x.com");
        assertThat(s.bloqueio("1.1.1.1", "admin@x.com")).isEqualTo(Duration.ofMinutes(15));
        // Outro IP ainda consegue tentar (a conta só bloqueia com 20 falhas).
        assertThat(s.bloqueio("2.2.2.2", "admin@x.com")).isZero();

        relogio.agora = relogio.agora.plus(Duration.ofMinutes(16));
        assertThat(s.bloqueio("1.1.1.1", "admin@x.com")).isZero();
    }

    @Test
    void bloqueiaContaComMuitasFalhasDeIpsDiferentes() {
        LoginAttemptService s = new LoginAttemptService(new RelogioAjustavel(), 5, 20, 15, 15);

        for (int i = 0; i < 20; i++) {
            s.registrarFalha("10.0.0." + i, "Admin@X.com");
        }
        assertThat(s.bloqueio("99.99.99.99", "admin@x.com")).isPositive();
        assertThat(s.bloqueio("99.99.99.99", "outro@x.com")).isZero();
    }

    @Test
    void loginCertoZeraAsFalhas() {
        LoginAttemptService s = new LoginAttemptService(new RelogioAjustavel(), 5, 20, 15, 15);

        for (int i = 0; i < 4; i++) {
            s.registrarFalha("1.1.1.1", "admin@x.com");
        }
        s.registrarSucesso("1.1.1.1", "admin@x.com");
        s.registrarFalha("1.1.1.1", "admin@x.com");

        assertThat(s.bloqueio("1.1.1.1", "admin@x.com")).isZero();
    }
}
