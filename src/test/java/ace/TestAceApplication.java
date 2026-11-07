package ace;

import org.springframework.boot.SpringApplication;

/**
 * Sobe a aplicação com o classpath de testes e o perfil "h2"
 * (banco em memória + alunos de exemplo), sem precisar do PostgreSQL.
 *
 * Uso: mvnw spring-boot:test-run
 */
public class TestAceApplication {

    public static void main(String[] args) {
        SpringApplication.from(AceApplication::main)
            .withAdditionalProfiles("h2")
            .run(args);
    }
}
