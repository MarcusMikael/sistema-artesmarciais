package ace.controller;

import ace.dto.UsuarioLogado;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final String nomeAdmin;

    public AuthController(@Value("${app.admin.nome}") String nomeAdmin) {
        this.nomeAdmin = nomeAdmin;
    }

    // Dados do usuário logado.
    @GetMapping("/me")
    public UsuarioLogado usuarioLogado(Authentication authentication) {
        return new UsuarioLogado(authentication.getName(), nomeAdmin);
    }

    /** Força a geração do cookie XSRF-TOKEN usado nas requisições POST/PUT/PATCH/DELETE. */
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken token) {
        token.getToken();
        return ResponseEntity.noContent().build();
    }
}
