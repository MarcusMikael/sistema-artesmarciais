
package ace.controller;

import ace.model.PagamentoModel;
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
    public List<PagamentoModel> listarTodos() {
        return pagamentoService.listarTodos();
    }

    @GetMapping("/{id}")
    public PagamentoModel buscarPorId(@PathVariable Long id) {
        return pagamentoService.buscarPorId(id);
    }

    @GetMapping("/mes")
    public List<PagamentoModel> listarPorMesAno(
            @RequestParam Integer mes,
            @RequestParam Integer ano) {

        return pagamentoService.listarPorMesAno(mes, ano);
    }

    @GetMapping("/status/{status}")
    public List<PagamentoModel> listarPorStatus(
            @PathVariable StatusPagamento status) {

        return pagamentoService.listarPorStatus(status);
    }

    @PostMapping
    public ResponseEntity<PagamentoModel> criarPagamento(
            @RequestParam Long idAluno,
            @RequestParam Integer mes,
            @RequestParam Integer ano) {

        PagamentoModel pagamento = pagamentoService.criarPagamento(
                idAluno, mes, ano
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(pagamento);
    }

    /**
     * Gera as mensalidades PENDENTES do mês para todos os alunos ativos que
     * ainda não possuem pagamento e devolve a lista completa do mês.
     */
    @PostMapping("/gerar")
    public List<PagamentoModel> gerarMensalidades(
            @RequestParam Integer mes,
            @RequestParam Integer ano) {

        return pagamentoService.gerarMensalidades(mes, ano);
    }

    @PatchMapping("/{id}/comprovante")
    public PagamentoModel registrarComprovante(@PathVariable Long id) {
        return pagamentoService.registrarComprovante(id);
    }

    @PatchMapping("/{id}/confirmar")
    public PagamentoModel confirmarPagamento(@PathVariable Long id) {
        return pagamentoService.confirmarPagamento(id);
    }

    @PatchMapping("/{id}/desfazer-confirmacao")
    public PagamentoModel desfazerConfirmacao(@PathVariable Long id) {
        return pagamentoService.desfazerConfirmacao(id);
    }
}
