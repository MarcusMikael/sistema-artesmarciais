package ace.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Verificação de saúde, pública e leve (não consulta o banco).
 * Serve para um serviço de ping manter o Render acordado e como Health Check Path.
 */
@RestController
public class SaudeController {

    @GetMapping("/api/saude")
    public Map<String, String> saude() {
        return Map.of("status", "ok");
    }
}
