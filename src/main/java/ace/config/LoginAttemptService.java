package ace.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bloqueia temporariamente o login depois de muitas senhas erradas.
 *
 * <p>Conta as falhas por IP e por e-mail. O limite por IP é baixo (barra quem testa
 * senhas a partir de um endereço); o limite por e-mail é mais alto, para quem troca
 * de IP, sem deixar fácil um terceiro manter o admin bloqueado para sempre.</p>
 */
@Component
public class LoginAttemptService {

    private record Tentativas(int falhas, Instant inicio, Instant bloqueadoAte) {
    }

    private final Map<String, Tentativas> registros = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maxPorIp;
    private final int maxPorConta;
    private final Duration janela;
    private final Duration bloqueio;

    public LoginAttemptService(
            Clock clock,
            @Value("${app.login.max-falhas-por-ip:5}") int maxPorIp,
            @Value("${app.login.max-falhas-por-conta:20}") int maxPorConta,
            @Value("${app.login.janela-minutos:15}") long janelaMinutos,
            @Value("${app.login.bloqueio-minutos:15}") long bloqueioMinutos) {
        this.clock = clock;
        this.maxPorIp = maxPorIp;
        this.maxPorConta = maxPorConta;
        this.janela = Duration.ofMinutes(janelaMinutos);
        this.bloqueio = Duration.ofMinutes(bloqueioMinutos);
    }

    /** Tempo restante de bloqueio, ou {@link Duration#ZERO} se pode tentar. */
    public Duration bloqueio(String ip, String email) {
        Instant agora = clock.instant();
        Duration porIp = restante(chaveIp(ip), agora);
        Duration porConta = restante(chaveConta(email), agora);
        return porIp.compareTo(porConta) >= 0 ? porIp : porConta;
    }

    public void registrarFalha(String ip, String email) {
        Instant agora = clock.instant();
        falhar(chaveIp(ip), maxPorIp, agora);
        if (email != null && !email.isBlank()) {
            falhar(chaveConta(email), maxPorConta, agora);
        }
        limparAntigos(agora);
    }

    public void registrarSucesso(String ip, String email) {
        registros.remove(chaveIp(ip));
        registros.remove(chaveConta(email));
    }

    private void falhar(String chave, int limite, Instant agora) {
        registros.compute(chave, (k, atual) -> {
            if (atual == null || atual.inicio().plus(janela).isBefore(agora)) {
                atual = new Tentativas(0, agora, null);
            }
            int falhas = atual.falhas() + 1;
            Instant bloqueadoAte = falhas >= limite ? agora.plus(bloqueio) : atual.bloqueadoAte();
            return new Tentativas(falhas, atual.inicio(), bloqueadoAte);
        });
    }

    private Duration restante(String chave, Instant agora) {
        Tentativas t = registros.get(chave);
        if (t == null || t.bloqueadoAte() == null || !t.bloqueadoAte().isAfter(agora)) {
            return Duration.ZERO;
        }
        return Duration.between(agora, t.bloqueadoAte());
    }

    // Evita que o mapa cresça sem limite com IPs que nunca voltam.
    private void limparAntigos(Instant agora) {
        if (registros.size() < 1_000) {
            return;
        }
        registros.entrySet().removeIf(e -> {
            Tentativas t = e.getValue();
            Instant fim = t.bloqueadoAte() != null ? t.bloqueadoAte() : t.inicio().plus(janela);
            return fim.isBefore(agora);
        });
    }

    private static String chaveIp(String ip) {
        return "ip:" + ip;
    }

    private static String chaveConta(String email) {
        return "conta:" + (email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
    }
}
