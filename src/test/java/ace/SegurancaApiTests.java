package ace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("h2")
class SegurancaApiTests {

    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void apiExigeLogin() throws Exception {
        mvc.perform(get("/api/alunos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/login.html")).andExpect(status().isOk());
    }

    @Test
    void loginComSenhaCertaEErrada() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf())
                .param("email", "admin@alessandrosilva.com").param("senha", "errada"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("E-mail ou senha incorretos."));

        mvc.perform(post("/api/auth/login").with(csrf())
                .param("email", "admin@alessandrosilva.com").param("senha", "alessandro123"))
            .andExpect(status().isNoContent());
    }

    @Test
    void loginBloqueiaDepoisDeCincoSenhasErradas() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").with(csrf()).with(ip("203.0.113.7"))
                    .param("email", "admin@alessandrosilva.com").param("senha", "errada" + i))
                .andExpect(status().isUnauthorized());
        }
        // Mesmo com a senha certa, o IP fica bloqueado por um tempo.
        mvc.perform(post("/api/auth/login").with(csrf()).with(ip("203.0.113.7"))
                .param("email", "admin@alessandrosilva.com").param("senha", "alessandro123"))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Muitas tentativas")));
    }

    @Test
    void respostasTrazemCabecalhosDeSeguranca() throws Exception {
        mvc.perform(get("/login.html"))
            .andExpect(header().string("Content-Security-Policy",
                org.hamcrest.Matchers.containsString("script-src 'self'")))
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(header().string("Referrer-Policy", "same-origin"))
            .andExpect(header().exists("Permissions-Policy"));
    }

    @Test
    @WithMockUser
    void mensalidadeComDigitosDemaisEhRecusada() throws Exception {
        mvc.perform(post("/api/alunos").with(csrf())
                .contentType("application/json")
                .content("{\"nome\":\"Aluno Teste\",\"modalidade\":\"JIU_JITSU\",\"valorMensalidade\":99999999999}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.valorMensalidade").exists());
        mvc.perform(get("/api/pagamentos/mes").param("mes", "1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Parâmetro obrigatório ausente: ano."));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor ip(String endereco) {
        return request -> {
            request.setRemoteAddr(endereco);
            return request;
        };
    }

    @Test
    @WithMockUser
    void escritaSemTokenCsrfEhBloqueada() throws Exception {
        mvc.perform(post("/api/alunos").contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void validacaoDevolveCamposComErro() throws Exception {
        mvc.perform(post("/api/alunos").with(csrf())
                .contentType("application/json").content("{\"nome\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.nome").exists())
            .andExpect(jsonPath("$.fields.modalidade").exists());
    }

    @Test
    @WithMockUser
    void telefoneSemDddEhRecusado() throws Exception {
        mvc.perform(post("/api/alunos").with(csrf())
                .contentType("application/json")
                .content("{\"nome\":\"Aluno Teste\",\"telefone\":\"9999-0000\",\"modalidade\":\"JIU_JITSU\",\"valorMensalidade\":150}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.telefone").exists());
    }

    @Test
    @WithMockUser
    void relatorioEGraduacoesExigemDadosValidos() throws Exception {
        mvc.perform(get("/api/relatorios/receita").param("meses", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get("/api/relatorios/receita").param("meses", "abc"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/alunos/999999/graduacoes")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void listaPagamentosDoMesSerializaAluno() throws Exception {
        java.time.LocalDate hoje = java.time.LocalDate.now();
        mvc.perform(get("/api/pagamentos/mes")
                .param("mes", String.valueOf(hoje.getMonthValue()))
                .param("ano", String.valueOf(hoje.getYear())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].aluno.nome").exists())
            .andExpect(jsonPath("$[0].dataVencimento").exists())
            .andExpect(jsonPath("$[0].atrasado").isBoolean());
    }

    @Test
    @WithMockUser
    void alunoInexistenteDevolve404() throws Exception {
        mvc.perform(get("/api/alunos/999999")).andExpect(status().isNotFound());
    }
}
