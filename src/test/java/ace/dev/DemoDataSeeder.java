package ace.dev;

import ace.dto.GraduacaoRequest;
import ace.model.AlunoModel;
import ace.model.enums.Modalidade;
import ace.model.enums.StatusAluno;
import ace.repository.AlunoRepository;
import ace.service.GraduacaoService;
import ace.service.PagamentoService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/** Popula o banco H2 com alunos de exemplo (somente no perfil "h2"). */
@Component
@Profile("h2")
public class DemoDataSeeder implements CommandLineRunner {

    private final AlunoRepository alunoRepository;
    private final PagamentoService pagamentoService;
    private final GraduacaoService graduacaoService;

    public DemoDataSeeder(AlunoRepository alunoRepository, PagamentoService pagamentoService,
                          GraduacaoService graduacaoService) {
        this.alunoRepository = alunoRepository;
        this.pagamentoService = pagamentoService;
        this.graduacaoService = graduacaoService;
    }

    @Override
    public void run(String... args) {
        if (alunoRepository.count() > 0) {
            return;
        }
        LocalDate hoje = pagamentoService.hoje();

        AlunoModel lucas = aluno("Lucas Almeida", "(11) 98880-0101", Modalidade.JIU_JITSU, "Seg / Qua · 19h", "Branca", "180.00", 10, hoje.minusMonths(14), StatusAluno.ATIVO);
        aluno("Camila Santos", "(11) 98880-0102", Modalidade.KICKBOXING, "Ter / Qui · 18h", null, "160.00", 5, hoje.minusMonths(8), StatusAluno.ATIVO);
        AlunoModel rafael = aluno("Rafael Costa", "(11) 98880-0103", Modalidade.LUTA_LIVRE, "Seg / Qua · 20h", "Branca", "180.00", 15, hoje.minusMonths(30), StatusAluno.ATIVO);
        aluno("Beatriz Lima", "(11) 98880-0104", Modalidade.JIU_JITSU, "Ter / Qui · 19h", "Branca", "180.00", 10, hoje.minusMonths(3), StatusAluno.ATIVO);
        aluno("Pedro Rocha", "(11) 98880-0105", Modalidade.KICKBOXING, "Seg / Qua · 18h", null, "150.00", 20, hoje.minusMonths(6), StatusAluno.ATIVO);
        aluno("João Ferreira", "(11) 98880-0107", Modalidade.JIU_JITSU, "Sáb · 10h", "Cinza", "130.00", 1, hoje.minusMonths(2), StatusAluno.ATIVO);
        aluno("Marina Oliveira", "(11) 98880-0106", Modalidade.LUTA_LIVRE, "Ter / Qui · 18h", "Amarela", "180.00", 10, hoje.minusMonths(20), StatusAluno.INATIVO);

        graduacaoService.registrar(lucas.getId(), new GraduacaoRequest("Cinza", hoje.minusMonths(10), null));
        graduacaoService.registrar(lucas.getId(), new GraduacaoRequest("Azul", hoje.minusMonths(2), "Exame de faixa do semestre"));
        graduacaoService.registrar(rafael.getId(), new GraduacaoRequest("Azul", hoje.minusMonths(20), null));
        graduacaoService.registrar(rafael.getId(), new GraduacaoRequest("Roxa", hoje.minusMonths(4), null));

        // Meses anteriores: quase tudo pago, para o relatório ter histórico.
        YearMonth atual = YearMonth.from(hoje);
        for (int i = 5; i >= 1; i--) {
            YearMonth ref = atual.minusMonths(i);
            for (var p : pagamentoService.gerarMensalidades(ref.getMonthValue(), ref.getYear())) {
                boolean deixaPendente = i == 1 && p.getAluno().getNome().startsWith("João");
                if (!deixaPendente) {
                    pagamentoService.registrarComprovante(p.getId());
                    pagamentoService.confirmarPagamento(p.getId());
                }
            }
        }

        // Mês atual: Lucas e Beatriz com comprovante; Rafael e Pedro pagos;
        // Camila e João pendentes (vencimentos nos dias 5 e 1).
        for (var p : pagamentoService.gerarMensalidades(atual.getMonthValue(), atual.getYear())) {
            String nome = p.getAluno().getNome();
            if (nome.startsWith("Lucas") || nome.startsWith("Beatriz")) {
                pagamentoService.registrarComprovante(p.getId());
            } else if (nome.startsWith("Rafael") || nome.startsWith("Pedro")) {
                pagamentoService.registrarComprovante(p.getId());
                pagamentoService.confirmarPagamento(p.getId());
            }
        }
    }

    private AlunoModel aluno(String nome, String telefone, Modalidade modalidade, String turma,
                             String faixa, String valor, int diaVencimento, LocalDate matricula,
                             StatusAluno status) {
        AlunoModel a = new AlunoModel();
        a.setNome(nome);
        a.setTelefone(telefone);
        a.setModalidade(modalidade);
        a.setTurmaHorario(turma);
        a.setFaixaAtual(faixa);
        a.setValorMensalidade(new BigDecimal(valor));
        a.setDiaVencimento(diaVencimento);
        a.setDataMatricula(matricula);
        a.setStatus(status);
        return alunoRepository.save(a);
    }
}
