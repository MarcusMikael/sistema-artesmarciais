package ace.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String LOGIN_URL = "/api/auth/login";
    private static final int DIAS_LEMBRAR = 30;

    /** Senhas que já foram publicadas no repositório (README / arquivo de exemplo). */
    private static final Set<String> SENHAS_PUBLICADAS = Set.of("alessandro123");
    private static final int TAMANHO_MINIMO_SENHA = 10;

    /*
     * Só scripts, fontes e dados do próprio sistema. Estilos inline são necessários
     * para larguras/alturas calculadas (barras dos gráficos) e cores das faixas.
     */
    private static final String CSP = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",
            "font-src 'self'",
            "img-src 'self' data:",
            "connect-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   LoginAttemptService tentativas,
                                                   TokenLembrarRepository tokensLembrar,
                                                   UserDetailsService usuarios,
                                                   @Value("${app.lembrar.chave:${app.admin.email}}") String chaveLembrar)
            throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/index.html", "/login.html",
                    "/css/**", "/js/**", "/img/**", "/fonts/**",
                    "/favicon.ico", "/error",
                    "/api/auth/csrf", "/api/saude"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(new BloqueioLoginFilter(tentativas), UsernamePasswordAuthenticationFilter.class)
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl(LOGIN_URL)
                .usernameParameter("email")
                .passwordParameter("senha")
                .successHandler((request, response, authentication) -> {
                    tentativas.registrarSucesso(request.getRemoteAddr(), request.getParameter("email"));
                    response.setStatus(HttpStatus.NO_CONTENT.value());
                })
                .failureHandler((request, response, exception) -> {
                    tentativas.registrarFalha(request.getRemoteAddr(), request.getParameter("email"));
                    escreverJson(response, HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos.");
                })
                .permitAll()
            )
            // "Lembrar de mim": token guardado no banco, válido por 30 dias e trocado a cada
            // uso. Sobrevive aos reinícios do Render; o logout apaga o token.
            .rememberMe(rm -> rm
                .tokenRepository(tokensLembrar)
                .userDetailsService(usuarios)
                .key(chaveLembrar)
                .rememberMeParameter("lembrar")
                .rememberMeCookieName("lembrar_me")
                .tokenValiditySeconds(DIAS_LEMBRAR * 24 * 60 * 60)
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                // Apaga o token do "Lembrar de mim" mesmo quando o logout chega sem sessão.
                .addLogoutHandler((request, response, authentication) -> {
                    if (request.getCookies() == null) {
                        return;
                    }
                    for (jakarta.servlet.http.Cookie c : request.getCookies()) {
                        if ("lembrar_me".equals(c.getName())) {
                            tokensLembrar.removerPeloCookie(c.getValue());
                        }
                    }
                })
                .logoutSuccessHandler((request, response, authentication) ->
                    response.setStatus(HttpStatus.NO_CONTENT.value()))
            )
            .exceptionHandling(ex -> ex
                // Chamadas da API recebem 401 em vez de redirecionamento.
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    PathPatternRequestMatcher.withDefaults().matcher("/api/**"))
            )
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(CSP))
                .referrerPolicy(ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                    "camera=(), microphone=(), geolocation=(), payment=()"))
            )
            // Token CSRF em cookie (XSRF-TOKEN) lido pelo JavaScript e
            // devolvido no cabeçalho X-XSRF-TOKEN.
            .csrf(csrf -> csrf.spa());

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(
            @Value("${app.admin.email}") String email,
            @Value("${app.admin.password:}") String senha,
            @Value("${app.admin.permitir-senha-fraca:false}") boolean permitirSenhaFraca,
            PasswordEncoder passwordEncoder) {

        validarSenha(senha, permitirSenhaFraca);

        return new InMemoryUserDetailsManager(
            User.withUsername(email)
                .password(passwordEncoder.encode(senha))
                .roles("ADMIN")
                .build()
        );
    }

    /** Impede subir o sistema com a senha publicada no repositório ou com senha curta. */
    static void validarSenha(String senha, boolean permitirSenhaFraca) {
        if (senha == null || senha.isBlank()) {
            throw new IllegalStateException(
                "Defina a senha do administrador na variável de ambiente ADMIN_PASSWORD.");
        }
        if (permitirSenhaFraca) {
            return;
        }
        if (SENHAS_PUBLICADAS.contains(senha)) {
            throw new IllegalStateException(
                "ADMIN_PASSWORD usa a senha padrão que está publicada no repositório. Escolha outra.");
        }
        if (senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new IllegalStateException(
                "ADMIN_PASSWORD precisa ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    private static void escreverJson(HttpServletResponse response, HttpStatus status, String mensagem)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"message\":\"" + mensagem + "\"}");
    }

    /** Recusa tentativas de login enquanto o IP ou a conta estiverem bloqueados. */
    static final class BloqueioLoginFilter extends OncePerRequestFilter {

        private final LoginAttemptService tentativas;

        BloqueioLoginFilter(LoginAttemptService tentativas) {
            this.tentativas = tentativas;
        }

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            String caminho = request.getRequestURI().substring(request.getContextPath().length());
            return !("POST".equals(request.getMethod()) && LOGIN_URL.equals(caminho));
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain chain) throws ServletException, IOException {
            Duration espera = tentativas.bloqueio(request.getRemoteAddr(), request.getParameter("email"));
            if (espera.isZero()) {
                chain.doFilter(request, response);
                return;
            }
            long minutos = Math.max(1, (espera.toSeconds() + 59) / 60);
            response.setHeader("Retry-After", String.valueOf(espera.toSeconds()));
            escreverJson(response, HttpStatus.TOO_MANY_REQUESTS,
                "Muitas tentativas de login. Tente novamente em " + minutos
                    + (minutos == 1 ? " minuto." : " minutos."));
        }
    }
}
