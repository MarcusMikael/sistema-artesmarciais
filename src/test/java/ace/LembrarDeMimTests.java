package ace;

import ace.repository.LoginLembradoRepository;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("h2")
class LembrarDeMimTests {

    private static final String EMAIL = "admin@alessandrosilva.com";
    private static final String SENHA = "alessandro123";

    @Autowired WebApplicationContext context;
    @Autowired LoginLembradoRepository tokens;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        tokens.deleteAll();
    }

    private static RequestPostProcessor ip(String endereco) {
        return request -> {
            request.setRemoteAddr(endereco);
            return request;
        };
    }

    private Cookie loginLembrando() throws Exception {
        MvcResult login = mvc.perform(post("/api/auth/login").with(csrf()).with(ip("198.51.100.10"))
                .param("email", EMAIL).param("senha", SENHA).param("lembrar", "on"))
            .andExpect(status().isNoContent())
            .andExpect(cookie().exists("lembrar_me"))
            .andReturn();
        Cookie lembrar = login.getResponse().getCookie("lembrar_me");
        assertThat(lembrar.getMaxAge()).isEqualTo(30 * 24 * 60 * 60);
        assertThat(lembrar.isHttpOnly()).isTrue();
        return lembrar;
    }

    @Test
    void lembrarDeMimAutenticaSemSessaoEGuardaTokenNoBanco() throws Exception {
        Cookie lembrar = loginLembrando();
        assertThat(tokens.countByUsuario(EMAIL)).isEqualTo(1);

        // Requisição nova, sem sessão (como depois de o servidor reiniciar): só o cookie.
        mvc.perform(get("/api/alunos").cookie(lembrar).with(ip("198.51.100.10")))
            .andExpect(status().isOk());
    }

    @Test
    void semMarcarLembrarNaoCriaCookieNemToken() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).with(ip("198.51.100.11"))
                .param("email", EMAIL).param("senha", SENHA))
            .andExpect(status().isNoContent())
            .andExpect(cookie().doesNotExist("lembrar_me"));
        assertThat(tokens.count()).isZero();
    }

    @Test
    void logoutApagaOTokenEOCookieDeixaDeValer() throws Exception {
        Cookie lembrar = loginLembrando();

        // O token é trocado a cada uso: o logout usa o cookie mais recente.
        MvcResult uso = mvc.perform(get("/api/alunos").cookie(lembrar).with(ip("198.51.100.12")))
            .andExpect(status().isOk())
            .andReturn();
        Cookie atual = uso.getResponse().getCookie("lembrar_me");
        assertThat(atual).isNotNull();

        mvc.perform(post("/api/auth/logout").with(csrf()).cookie(atual).with(ip("198.51.100.12")))
            .andExpect(status().isNoContent());

        assertThat(tokens.countByUsuario(EMAIL)).isZero();
        mvc.perform(get("/api/alunos").cookie(atual).with(ip("198.51.100.12")))
            .andExpect(status().isUnauthorized());
    }
}
