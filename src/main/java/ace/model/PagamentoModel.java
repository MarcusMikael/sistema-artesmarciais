package ace.model;

import ace.model.enums.StatusPagamento;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "pagamento",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_pagamento_aluno_mes_ano",
        columnNames = {"id_aluno", "mes_referencia", "ano_referencia"}
    )
)
public class PagamentoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relacionamento com Aluno
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JoinColumn(name = "id_aluno", nullable = false)
    private AlunoModel aluno;

    @Column(name = "mes_referencia", nullable = false)
    private Integer mesReferencia;

    @Column(name = "ano_referencia", nullable = false)
    private Integer anoReferencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_pagamento", nullable = false)
    private StatusPagamento statusPagamento = StatusPagamento.PENDENTE;

    @Column(name = "comprovante_recebido", nullable = false)
    private Boolean comprovanteRecebido = false;

    @Column(name = "data_hora_confirmacao")
    private LocalDateTime dataHoraConfirmacao;

    // Valor cobrado nesta mensalidade, gravado na geração. Fica nulo apenas em
    // registros antigos, criados antes desta coluna existir.
    @Column(name = "valor", precision = 10, scale = 2)
    private BigDecimal valor;

    public PagamentoModel() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AlunoModel getAluno() {
        return aluno;
    }

    public void setAluno(AlunoModel aluno) {
        this.aluno = aluno;
    }

    public Integer getMesReferencia() {
        return mesReferencia;
    }

    public void setMesReferencia(Integer mesReferencia) {
        this.mesReferencia = mesReferencia;
    }

    public Integer getAnoReferencia() {
        return anoReferencia;
    }

    public void setAnoReferencia(Integer anoReferencia) {
        this.anoReferencia = anoReferencia;
    }

    public StatusPagamento getStatusPagamento() {
        return statusPagamento;
    }

    public void setStatusPagamento(StatusPagamento statusPagamento) {
        this.statusPagamento = statusPagamento;
    }

    public Boolean getComprovanteRecebido() {
        return comprovanteRecebido;
    }

    public void setComprovanteRecebido(Boolean comprovanteRecebido) {
        this.comprovanteRecebido = comprovanteRecebido;
    }

    public LocalDateTime getDataHoraConfirmacao() {
        return dataHoraConfirmacao;
    }

    public void setDataHoraConfirmacao(LocalDateTime dataHoraConfirmacao) {
        this.dataHoraConfirmacao = dataHoraConfirmacao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }
}