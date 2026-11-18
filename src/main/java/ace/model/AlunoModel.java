package ace.model;

import ace.model.enums.Modalidade;
import ace.model.enums.StatusAluno;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "aluno")
public class AlunoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Informe o nome completo.")
    @Size(min = 3, max = 150, message = "Informe o nome completo.")
    @Column(nullable = false, length = 150)
    private String nome;

    @Size(max = 20, message = "Telefone muito longo.")
    @Column(length = 20)
    private String telefone;

    @NotNull(message = "Selecione a modalidade.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Modalidade modalidade;

    @Column(name = "turma_horario", length = 100)
    private String turmaHorario;

    @Column(name = "faixa_atual", length = 50)
    private String faixaAtual;

    @NotNull(message = "Informe uma mensalidade válida.")
    @DecimalMin(value = "0.01", message = "Informe uma mensalidade válida.")
    @Column(name = "valor_mensalidade",
            nullable = false, precision = 10, scale = 2)
    private BigDecimal valorMensalidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAluno status = StatusAluno.ATIVO;

    /** Dia do mês em que a mensalidade vence (1 a 28). Nulo em cadastros antigos. */
    @Column(name = "dia_vencimento")
    private Integer diaVencimento;

    @Column(name = "data_matricula")
    private LocalDate dataMatricula;

    public static final int DIA_VENCIMENTO_PADRAO = 10;

    public AlunoModel() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public Modalidade getModalidade() {
        return modalidade;
    }

    public void setModalidade(Modalidade modalidade) {
        this.modalidade = modalidade;
    }

    public String getTurmaHorario() {
        return turmaHorario;
    }

    public void setTurmaHorario(String turmaHorario) {
        this.turmaHorario = turmaHorario;
    }

    public String getFaixaAtual() {
        return faixaAtual;
    }

    public void setFaixaAtual(String faixaAtual) {
        this.faixaAtual = faixaAtual;
    }

    public BigDecimal getValorMensalidade() {
        return valorMensalidade;
    }

    public void setValorMensalidade(BigDecimal valorMensalidade) {
        this.valorMensalidade = valorMensalidade;
    }

    public StatusAluno getStatus() {
        return status;
    }

    public void setStatus(StatusAluno status) {
        this.status = status;
    }

    public Integer getDiaVencimento() {
        return diaVencimento;
    }

    public void setDiaVencimento(Integer diaVencimento) {
        this.diaVencimento = diaVencimento;
    }

    /** Dia de vencimento, usando o padrão para cadastros antigos sem o campo. */
    public int diaVencimentoEfetivo() {
        return diaVencimento != null ? diaVencimento : DIA_VENCIMENTO_PADRAO;
    }

    public LocalDate getDataMatricula() {
        return dataMatricula;
    }

    public void setDataMatricula(LocalDate dataMatricula) {
        this.dataMatricula = dataMatricula;
    }
}
