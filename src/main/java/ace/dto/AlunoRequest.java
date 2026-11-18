package ace.dto;

import ace.model.AlunoModel;
import ace.model.enums.Modalidade;
import ace.model.enums.StatusAluno;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AlunoRequest(

        @NotBlank(message = "Informe o nome completo.")
        @Size(min = 3, max = 150, message = "Informe o nome completo.")
        String nome,

        // Opcional; se informado, precisa ter DDD (10 ou 11 dígitos, com ou sem máscara).
        @Size(max = 20, message = "Telefone muito longo.")
        @Pattern(regexp = "^$|^\\D*(\\d\\D*){10,11}$", message = "Informe um telefone com DDD.")
        String telefone,

        @NotNull(message = "Selecione a modalidade.")
        Modalidade modalidade,

        @Size(max = 100, message = "Turma/horário muito longo.")
        String turmaHorario,

        @Size(max = 50, message = "Faixa muito longa.")
        String faixaAtual,

        @NotNull(message = "Informe uma mensalidade válida.")
        @DecimalMin(value = "0.01", message = "Informe uma mensalidade válida.")
        @Digits(integer = 8, fraction = 2, message = "Informe uma mensalidade válida.")
        BigDecimal valorMensalidade,

        /* se vier nulo, o aluno é criado como ATIVO / mantém o status atual. */
        StatusAluno status,

        /* se vier nulo, usa o dia padrão (10) / mantém o dia atual. */
        @Min(value = 1, message = "Escolha um dia entre 1 e 28.")
        @Max(value = 28, message = "Escolha um dia entre 1 e 28.")
        Integer diaVencimento,

        /* se vier nulo, usa a data de hoje / mantém a data atual. */
        @PastOrPresent(message = "A matrícula não pode ser no futuro.")
        LocalDate dataMatricula
) {

    /** Converte o DTO para a entidade que o service sabe salvar. */
    public AlunoModel paraModel() {
        AlunoModel aluno = new AlunoModel();
        aluno.setNome(nome.trim());
        aluno.setTelefone(vazioParaNulo(telefone));
        aluno.setModalidade(modalidade);
        aluno.setTurmaHorario(vazioParaNulo(turmaHorario));
        aluno.setFaixaAtual(vazioParaNulo(faixaAtual));
        aluno.setValorMensalidade(valorMensalidade);
        aluno.setStatus(status);
        aluno.setDiaVencimento(diaVencimento);
        aluno.setDataMatricula(dataMatricula);
        return aluno;
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
