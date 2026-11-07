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
    void listaPagamentosDoMesSerializaAluno() throws Exception {
        java.time.LocalDate hoje = java.time.LocalDate.now();
        mvc.perform(get("/api/pagamentos/mes")
                .param("mes", String.valueOf(hoje.getMonthValue()))
                .param("ano", String.valueOf(hoje.getYear())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].aluno.nome").exists());
    }

    @Test
    @WithMockUser
    void alunoInexistenteDevolve404() throws Exception {
        mvc.perform(get("/api/alunos/999999")).andExpect(status().isNotFound());
    }
}
