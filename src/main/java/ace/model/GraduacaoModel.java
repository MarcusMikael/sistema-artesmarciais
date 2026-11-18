package ace.model;

import jakarta.persistence.*;

import java.time.LocalDate;

/** Uma troca de faixa/graduação do aluno. */
@Entity
@Table(name = "graduacao")
public class GraduacaoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_aluno", nullable = false)
    private AlunoModel aluno;

    @Column(nullable = false, length = 50)
    private String faixa;

    @Column(nullable = false)
    private LocalDate data;

    @Column(length = 255)
    private String observacao;

    public GraduacaoModel() {
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

    public String getFaixa() {
        return faixa;
    }

    public void setFaixa(String faixa) {
        this.faixa = faixa;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }
}
