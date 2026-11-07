package ace;

import ace.model.AlunoModel;
import ace.model.PagamentoModel;
import ace.model.enums.Modalidade;
import ace.model.enums.StatusAluno;
import ace.model.enums.StatusPagamento;
import ace.repository.PagamentoRepository;
import ace.service.AlunoService;
import ace.service.PagamentoService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class RegrasNegocioTests {

    // Mês sem dados do DemoDataSeeder (que usa o mês atual).
    private static final int MES = 1;
    private static final int ANO = 2001;

    @Autowired AlunoService alunoService;
    @Autowired PagamentoService pagamentoService;
    @Autowired PagamentoRepository pagamentoRepository;

    private AlunoModel novoAluno(String nome, StatusAluno status) {
        AlunoModel a = new AlunoModel();
        a.setNome(nome);
        a.setTelefone("(11) 90000-0000");
        a.setModalidade(Modalidade.JIU_JITSU);
        a.setTurmaHorario("Seg / Qua · 19h");
        a.setFaixaAtual("Branca");
        a.setValorMensalidade(new BigDecimal("150.00"));
        a.setStatus(status);
        return alunoService.cadastrar(a);
    }

    private PagamentoModel pagamentoDe(List<PagamentoModel> lista, AlunoModel aluno) {
        return lista.stream()
            .filter(p -> p.getAluno().getId().equals(aluno.getId()))
            .findFirst().orElseThrow();
    }

    @Test
    void gerarMensalidadesCriaSoParaAtivosESemDuplicar() {
        AlunoModel ativo = novoAluno("Aluno Ativo", StatusAluno.ATIVO);
        AlunoModel inativo = novoAluno("Aluno Inativo", StatusAluno.INATIVO);

        List<PagamentoModel> primeira = pagamentoService.gerarMensalidades(MES, ANO);
        List<PagamentoModel> segunda = pagamentoService.gerarMensalidades(MES, ANO);

        assertThat(primeira).anyMatch(p -> p.getAluno().getId().equals(ativo.getId()));
        assertThat(primeira).noneMatch(p -> p.getAluno().getId().equals(inativo.getId()));
        assertThat(segunda).hasSameSizeAs(primeira);
    }

    @Test
    void fluxoComprovanteConfirmacaoEDesfazer() {
        AlunoModel aluno = novoAluno("Aluno Fluxo", StatusAluno.ATIVO);
        PagamentoModel p = pagamentoDe(pagamentoService.gerarMensalidades(MES, ANO), aluno);

        assertThatThrownBy(() -> pagamentoService.confirmarPagamento(p.getId()))
            .isInstanceOf(IllegalStateException.class);

        pagamentoService.registrarComprovante(p.getId());
        PagamentoModel pago = pagamentoService.confirmarPagamento(p.getId());
        assertThat(pago.getStatusPagamento()).isEqualTo(StatusPagamento.PAGO);
        assertThat(pago.getDataHoraConfirmacao()).isNotNull();

        PagamentoModel desfeito = pagamentoService.desfazerConfirmacao(p.getId());
        assertThat(desfeito.getStatusPagamento()).isEqualTo(StatusPagamento.AGUARDANDO_CONFIRMACAO);
        assertThat(desfeito.getDataHoraConfirmacao()).isNull();
    }

    @Test
    void excluirAlunoSomenteComMensalidadesPendentes() {
        AlunoModel semHistorico = novoAluno("Sem Historico", StatusAluno.ATIVO);
        AlunoModel comHistorico = novoAluno("Com Historico", StatusAluno.ATIVO);
        List<PagamentoModel> lista = pagamentoService.gerarMensalidades(MES, ANO);
        pagamentoService.registrarComprovante(pagamentoDe(lista, comHistorico).getId());

        alunoService.excluir(semHistorico.getId());
        assertThat(pagamentoRepository.existsByAluno_Id(semHistorico.getId())).isFalse();

        assertThatThrownBy(() -> alunoService.excluir(comHistorico.getId()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("histórico financeiro");
    }

    @Test
    void reajusteNaoAlteraPagamentosConferidosMasAtualizaPendentes() {
        AlunoModel aluno = novoAluno("Aluno Reajuste", StatusAluno.ATIVO);
        PagamentoModel confirmado = pagamentoDe(pagamentoService.gerarMensalidades(MES, ANO), aluno);
        assertThat(confirmado.getValor()).isEqualByComparingTo("150.00");
        pagamentoService.registrarComprovante(confirmado.getId());
        pagamentoService.confirmarPagamento(confirmado.getId());

        PagamentoModel pendente = pagamentoDe(pagamentoService.gerarMensalidades(MES + 1, ANO), aluno);

        AlunoModel dados = alunoService.buscarPorId(aluno.getId());
        dados.setValorMensalidade(new BigDecimal("200.00"));
        alunoService.atualizar(aluno.getId(), dados);

        assertThat(pagamentoService.buscarPorId(confirmado.getId()).getValor())
            .isEqualByComparingTo("150.00");
        assertThat(pagamentoService.buscarPorId(pendente.getId()).getValor())
            .isEqualByComparingTo("200.00");
    }

    @Test
    void registroAntigoSemValorCongelaNoComprovante() {
        AlunoModel aluno = novoAluno("Aluno Legado", StatusAluno.ATIVO);
        PagamentoModel p = pagamentoDe(pagamentoService.gerarMensalidades(MES, ANO), aluno);
        p.setValor(null); // simula linha criada antes da coluna existir
        pagamentoRepository.saveAndFlush(p);

        assertThat(pagamentoService.registrarComprovante(p.getId()).getValor())
            .isEqualByComparingTo("150.00");
    }

    @Test
    void naoGeraPagamentoManualDuplicadoNemParaInativo() {
        AlunoModel ativo = novoAluno("Manual Ativo", StatusAluno.ATIVO);
        AlunoModel inativo = novoAluno("Manual Inativo", StatusAluno.INATIVO);

        pagamentoService.criarPagamento(ativo.getId(), MES, ANO);
        assertThatThrownBy(() -> pagamentoService.criarPagamento(ativo.getId(), MES, ANO))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> pagamentoService.criarPagamento(inativo.getId(), MES, ANO))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> pagamentoService.gerarMensalidades(13, ANO))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
