package ace.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final String nomeAdmin;

    public AuthController(@Value("${app.admin.nome}") String nomeAdmin) {
        this.nomeAdmin = nomeAdmin;
    }

    // Dados do usuário logado.
    @GetMapping("/me")
    public Map<String, String> usuarioLogado(Authentication authentication) {
        return Map.of(
            "email", authentication.getName(),
            "nome", nomeAdmin
        );
    }

    /** Força a geração do cookie XSRF-TOKEN usado nas requisições POST/PUT/PATCH/DELETE. */
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken token) {
        token.getToken();
        return ResponseEntity.noContent().build();
    }
}
