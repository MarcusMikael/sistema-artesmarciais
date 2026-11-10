package ace.dto;

import ace.model.PagamentoModel;
import ace.model.enums.StatusPagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        BigDecimal valor
) {

    public static PagamentoResponse de(PagamentoModel p) {
        return new PagamentoResponse(
                p.getId(),
                AlunoResponse.de(p.getAluno()),
                p.getMesReferencia(),
                p.getAnoReferencia(),
                p.getStatusPagamento(),
                p.getComprovanteRecebido(),
                p.getDataHoraConfirmacao(),
                p.getValor());
    }

    public static List<PagamentoResponse> de(List<PagamentoModel> pagamentos) {
        return pagamentos.stream().map(PagamentoResponse::de).toList();
    }
}

