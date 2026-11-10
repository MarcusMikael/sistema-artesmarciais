package ace.dto;

import ace.model.AlunoModel;
import ace.model.enums.Modalidade;
import ace.model.enums.StatusAluno;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AlunoRequest(

        @NotBlank(message = "Informe o nome completo.")
        @Size(min = 3, max = 150, message = "Informe o nome completo.")
        String nome,

        @Size(max = 20, message = "Telefone muito longo.")
        String telefone,

        @NotNull(message = "Selecione a modalidade.")
        Modalidade modalidade,

        @Size(max = 100, message = "Turma/horário muito longo.")
        String turmaHorario,

        @Size(max = 50, message = "Faixa muito longa.")
        String faixaAtual,

        @NotNull(message = "Informe uma mensalidade válida.")
        @DecimalMin(value = "0.01", message = "Informe uma mensalidade válida.")
        BigDecimal valorMensalidade,

        /* se vier nulo, o aluno é criado como ATIVO / mantém o status atual. */
        StatusAluno status
) {

    /** Converte o DTO para a entidade que o service sabe salvar. */
    public AlunoModel paraModel() {
        AlunoModel aluno = new AlunoModel();
        aluno.setNome(nome);
        aluno.setTelefone(telefone);
        aluno.setModalidade(modalidade);
        aluno.setTurmaHorario(turmaHorario);
        aluno.setFaixaAtual(faixaAtual);
        aluno.setValorMensalidade(valorMensalidade);
        aluno.setStatus(status);
        return aluno;
    }
}
