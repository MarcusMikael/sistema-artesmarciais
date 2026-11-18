package ace.config;

import ace.model.LoginLembradoModel;
import ace.repository.LoginLembradoRepository;

import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

/**
 * Guarda os tokens do "Lembrar de mim" no banco (tabela login_lembrado).
 *
 * <p>No Render gratuito o servidor reinicia quando "acorda"; as sessões em memória se
 * perdem, mas estes tokens continuam válidos, então o admin não precisa logar de novo.
 * Cada uso troca o token (série fixa, token novo), o que permite ao Spring Security
 * detectar um cookie roubado e reutilizado.</p>
 */
@Component
public class TokenLembrarRepository implements PersistentTokenRepository {

    private final LoginLembradoRepository repository;

    public TokenLembrarRepository(LoginLembradoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void createNewToken(PersistentRememberMeToken token) {
        repository.save(new LoginLembradoModel(token.getSeries(), token.getUsername(),
                token.getTokenValue(), token.getDate().toInstant()));
    }

    @Override
    @Transactional
    public void updateToken(String series, String tokenValue, Date lastUsed) {
        repository.findById(series).ifPresent(t -> {
            t.setToken(tokenValue);
            t.setUltimoUso(lastUsed.toInstant());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public PersistentRememberMeToken getTokenForSeries(String seriesId) {
        return repository.findById(seriesId)
                .map(t -> new PersistentRememberMeToken(t.getUsuario(), t.getSerie(),
                        t.getToken(), Date.from(t.getUltimoUso())))
                .orElse(null);
    }

    @Override
    @Transactional
    public void removeUserTokens(String username) {
        repository.deleteByUsuario(username);
    }

    /**
     * Apaga o token indicado pelo valor do cookie (Base64 de "serie:token", com as
     * partes codificadas para URL).
     * Usado no logout quando o pedido chega só com o cookie, sem sessão aberta.
     */
    @Transactional
    public void removerPeloCookie(String valorCookie) {
        if (valorCookie == null || valorCookie.isBlank()) {
            return;
        }
        try {
            String base64 = valorCookie;
            while (base64.length() % 4 != 0) {
                base64 += "=";
            }
            String decodificado = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
            // Cada parte vem codificada para URL dentro do cookie (ex.: "%3D" no lugar de "=").
            String serie = URLDecoder.decode(decodificado.split(":", 2)[0], StandardCharsets.UTF_8);
            if (!serie.isBlank()) {
                repository.deleteById(serie);
            }
        } catch (IllegalArgumentException e) {
            // Cookie inválido: não há token para apagar.
        }
    }
}
