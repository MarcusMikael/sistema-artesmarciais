package ace.dto;

import ace.model.PagamentoModel;
import ace.model.enums.StatusPagamento;
import ace.service.PagamentoService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Dados do pagamento devolvidos pela API (inclui o aluno, como o front espera). */
public record PagamentoResponse(
        Long id,
        AlunoResponse aluno,
        Integer mesReferencia,
        Integer anoReferencia,
        StatusPagamento statusPagamento,
        Boolean comprovanteRecebido,
        LocalDateTime dataHoraConfirmacao,
        BigDecimal valor,
        LocalDate dataVencimento,
        boolean atrasado,
        long diasAtraso
) {

    /** @param hoje data de hoje no fuso da academia, usada para calcular o atraso. */
    public static PagamentoResponse de(PagamentoModel p, LocalDate hoje) {
        LocalDate vencimento = PagamentoService.vencimentoDe(p);
        boolean atrasado = p.getStatusPagamento() == StatusPagamento.PENDENTE
                && hoje.isAfter(vencimento);

        return new PagamentoResponse(
                p.getId(),
                AlunoResponse.de(p.getAluno()),
                p.getMesReferencia(),
                p.getAnoReferencia(),
                p.getStatusPagamento(),
                p.getComprovanteRecebido(),
                p.getDataHoraConfirmacao(),
                PagamentoService.valorDe(p),
                vencimento,
                atrasado,
                atrasado ? ChronoUnit.DAYS.between(vencimento, hoje) : 0);
    }

    public static List<PagamentoResponse> de(List<PagamentoModel> pagamentos, LocalDate hoje) {
        return pagamentos.stream().map(p -> de(p, hoje)).toList();
    }
}
