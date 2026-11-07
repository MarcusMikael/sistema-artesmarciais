package ace.dev;

import ace.model.AlunoModel;
import ace.model.enums.Modalidade;
import ace.model.enums.StatusAluno;
import ace.repository.AlunoRepository;
import ace.service.PagamentoService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Popula o banco H2 com alunos de exemplo (somente no perfil "h2"). */
@Component
@Profile("h2")
public class DemoDataSeeder implements CommandLineRunner {

    private final AlunoRepository alunoRepository;
    private final PagamentoService pagamentoService;

    public DemoDataSeeder(AlunoRepository alunoRepository, PagamentoService pagamentoService) {
        this.alunoRepository = alunoRepository;
        this.pagamentoService = pagamentoService;
    }

    @Override
    public void run(String... args) {
        if (alunoRepository.count() > 0) {
            return;
        }

        aluno("Lucas Almeida", "(11) 98880-0101", Modalidade.JIU_JITSU, "Seg / Qua · 19h", "Azul", StatusAluno.ATIVO);
        aluno("Camila Santos", "(11) 98880-0102", Modalidade.KICKBOXING, "Ter / Qui · 18h", "Não se aplica", StatusAluno.ATIVO);
        aluno("Rafael Costa", "(11) 98880-0103", Modalidade.LUTA_LIVRE, "Seg / Qua · 20h", "Roxa", StatusAluno.ATIVO);
        aluno("Beatriz Lima", "(11) 98880-0104", Modalidade.JIU_JITSU, "Ter / Qui · 19h", "Branca", StatusAluno.ATIVO);
        aluno("Pedro Rocha", "(11) 98880-0105", Modalidade.KICKBOXING, "Seg / Qua · 18h", "Não se aplica", StatusAluno.ATIVO);
        aluno("Marina Oliveira", "(11) 98880-0106", Modalidade.LUTA_LIVRE, "Ter / Qui · 18h", "Amarela", StatusAluno.INATIVO);

        LocalDate hoje = LocalDate.now();
        var pagamentos = pagamentoService.gerarMensalidades(hoje.getMonthValue(), hoje.getYear());

        // Lucas e Beatriz: comprovante recebido. Rafael e Pedro: já pagos.
        for (var p : pagamentos) {
            String nome = p.getAluno().getNome();
            if (nome.startsWith("Lucas") || nome.startsWith("Beatriz")) {
                pagamentoService.registrarComprovante(p.getId());
            } else if (nome.startsWith("Rafael") || nome.startsWith("Pedro")) {
                pagamentoService.registrarComprovante(p.getId());
                pagamentoService.confirmarPagamento(p.getId());
            }
        }
    }

    private void aluno(String nome, String telefone, Modalidade modalidade,
                       String turma, String faixa, StatusAluno status) {
        AlunoModel a = new AlunoModel();
        a.setNome(nome);
        a.setTelefone(telefone);
        a.setModalidade(modalidade);
        a.setTurmaHorario(turma);
        a.setFaixaAtual(faixa);
        a.setValorMensalidade(new BigDecimal("180.00"));
        a.setStatus(status);
        alunoRepository.save(a);
    }
}
