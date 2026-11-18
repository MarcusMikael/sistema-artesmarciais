package ace.dto;

import java.math.BigDecimal;

/** Resumo financeiro de um mês para o relatório de receita. */
public record ReceitaMensal(
        int mes,
        int ano,
        BigDecimal previsto,
        BigDecimal recebido,
        BigDecimal aguardando,
        BigDecimal pendente,
        int quantidade,
        int quantidadePagos
) {
}
