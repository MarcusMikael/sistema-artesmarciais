package ace.controller;

import ace.dto.ReceitaMensal;
import ace.service.PagamentoService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    private final PagamentoService pagamentoService;

    public RelatorioController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    // Receita dos últimos meses (o mais antigo primeiro), terminando no mês atual.
    @GetMapping("/receita")
    public ResponseEntity<List<ReceitaMensal>> receita(
            @RequestParam(defaultValue = "12") int meses) {
        return ResponseEntity.ok(pagamentoService.receitaMensal(meses));
    }
}
