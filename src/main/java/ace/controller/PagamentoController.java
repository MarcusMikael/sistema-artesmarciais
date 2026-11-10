package ace.controller;

import ace.dto.PagamentoResponse;
import ace.model.enums.StatusPagamento;
import ace.service.PagamentoService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pagamentos")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    public PagamentoController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    @GetMapping
    public ResponseEntity<List<PagamentoResponse>> listarTodos() {
        return ResponseEntity.ok(PagamentoResponse.de(pagamentoService.listarTodos()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PagamentoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(PagamentoResponse.de(pagamentoService.buscarPorId(id)));
    }

    @GetMapping("/mes")
    public ResponseEntity<List<PagamentoResponse>> listarPorMesAno(
            @RequestParam Integer mes,
            @RequestParam Integer ano) {

        return ResponseEntity.ok(
                PagamentoResponse.de(pagamentoService.listarPorMesAno(mes, ano)));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<PagamentoResponse>> listarPorStatus(
            @PathVariable StatusPagamento status) {

        return ResponseEntity.ok(
                PagamentoResponse.de(pagamentoService.listarPorStatus(status)));
    }

    @PostMapping
    public ResponseEntity<PagamentoResponse> criarPagamento(
            @RequestParam Long idAluno,
            @RequestParam Integer mes,
            @RequestParam Integer ano) {

        var pagamento = pagamentoService.criarPagamento(idAluno, mes, ano);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(PagamentoResponse.de(pagamento));
    }

    /*
      Gera as mensalidades PENDENTES do mês para todos os alunos ativos que
      ainda não possuem pagamento e devolve a lista completa do mês.
     */
    @PostMapping("/gerar")
    public ResponseEntity<List<PagamentoResponse>> gerarMensalidades(
            @RequestParam Integer mes,
            @RequestParam Integer ano) {

        return ResponseEntity.ok(
                PagamentoResponse.de(pagamentoService.gerarMensalidades(mes, ano)));
    }

    @PatchMapping("/{id}/comprovante")
    public ResponseEntity<PagamentoResponse> registrarComprovante(@PathVariable Long id) {
        return ResponseEntity.ok(
                PagamentoResponse.de(pagamentoService.registrarComprovante(id)));
    }

    @PatchMapping("/{id}/confirmar")
    public ResponseEntity<PagamentoResponse> confirmarPagamento(@PathVariable Long id) {
        return ResponseEntity.ok(
                PagamentoResponse.de(pagamentoService.confirmarPagamento(id)));
    }

    @PatchMapping("/{id}/desfazer-confirmacao")
    public ResponseEntity<PagamentoResponse> desfazerConfirmacao(@PathVariable Long id) {
        return ResponseEntity.ok(
                PagamentoResponse.de(pagamentoService.desfazerConfirmacao(id)));
    }
}
