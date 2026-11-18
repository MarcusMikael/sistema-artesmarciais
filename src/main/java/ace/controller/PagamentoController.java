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

    private java.time.LocalDate hoje() {
        return pagamentoService.hoje();
    }

    @GetMapping
    public ResponseEntity<List<PagamentoResponse>> listarTodos() {
        return ResponseEntity.ok(PagamentoResponse.de(pagamentoService.listarTodos(), hoje()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PagamentoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(PagamentoResponse.de(pagamentoService.buscarPorId(id), hoje()));
    }

    @GetMapping("/mes")
    public ResponseEntity<List<PagamentoResponse>> listarPorMesAno(
            @RequestParam Integer mes,
            @RequestParam Integer ano) {

        return ResponseEntity.ok(
                PagamentoResponse.de(pagamentoService.listarPorMesAno(mes, ano), hoje()));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<PagamentoResponse>> listarPorStatus(
            @PathVariable StatusPagamento status) {

        return ResponseEntity.ok(
                PagamentoResponse.de(pagamentoService.listarPorStatus(status), hoje()));
    }

    @PostMapping
    public ResponseEntity<PagamentoResponse> criarPagamento(
            @RequestParam Long idAluno,
            @RequestParam Integer mes,
            @RequestParam Integer ano) {

        var pagamento = pagamentoService.criarPagamento(idAluno, mes, ano);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(PagamentoResponse.de(pagamento, hoje()));
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
                PagamentoResponse.de(pagamentoService.gerarMensalidades(mes, ano), hoje()));
    }

    @PatchMapping("/{id}/comprovante")
    public ResponseEntity<PagamentoResponse> registrarComprovante(@PathVariable Long id) {
        return ResponseEntity.ok(
                PagamentoResponse.de(pagamentoService.registrarComprovante(id), hoje()));
    }

    @PatchMapping("/{id}/desfazer-comprovante")
    public ResponseEntity<PagamentoResponse> desfazerComprovante(@PathVariable Long id) {
        return ResponseEntity.ok(
                PagamentoResponse.de(pagamentoService.desfazerComprovante(id), hoje()));
    }

    @PatchMapping("/{id}/confirmar")
    public ResponseEntity<PagamentoResponse> confirmarPagamento(@PathVariable Long id) {
        return ResponseEntity.ok(
                PagamentoResponse.de(pagamentoService.confirmarPagamento(id), hoje()));
    }

    @PatchMapping("/{id}/desfazer-confirmacao")
    public ResponseEntity<PagamentoResponse> desfazerConfirmacao(@PathVariable Long id) {
        return ResponseEntity.ok(
                PagamentoResponse.de(pagamentoService.desfazerConfirmacao(id), hoje()));
    }
}
