package ace.dto;

import ace.model.AlunoModel;
import ace.model.enums.Modalidade;
import ace.model.enums.StatusAluno;

import java.math.BigDecimal;
import java.util.List;

public record AlunoResponse(
        Long id,
        String nome,
        String telefone,
        Modalidade modalidade,
        String turmaHorario,
        String faixaAtual,
        BigDecimal valorMensalidade,
        StatusAluno status
) {

    public static AlunoResponse de(AlunoModel a) {
        return new AlunoResponse(
                a.getId(),
                a.getNome(),
                a.getTelefone(),
                a.getModalidade(),
                a.getTurmaHorario(),
                a.getFaixaAtual(),
                a.getValorMensalidade(),
                a.getStatus());
    }

    public static List<AlunoResponse> de(List<AlunoModel> alunos) {
        return alunos.stream().map(AlunoResponse::de).toList();
    }
}

