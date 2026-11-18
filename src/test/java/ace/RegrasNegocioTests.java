package ace;

import ace.dto.GraduacaoRequest;
import ace.dto.PagamentoResponse;
import ace.dto.ReceitaMensal;
import ace.model.AlunoModel;
import ace.model.PagamentoModel;
import ace.model.enums.Modalidade;
import ace.model.enums.StatusAluno;
import ace.model.enums.StatusPagamento;
import ace.repository.PagamentoRepository;
import ace.service.AlunoService;
import ace.service.GraduacaoService;
import ace.service.PagamentoService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
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
    @Autowired GraduacaoService graduacaoService;

    private AlunoModel novoAluno(String nome, StatusAluno status) {
        AlunoModel a = new AlunoModel();
        a.setNome(nome);
        a.setTelefone("(11) 90000-0000");
        a.setModalidade(Modalidade.JIU_JITSU);
        a.setTurmaHorario("Seg / Qua · 19h");
        a.setFaixaAtual("Branca");
        a.setValorMensalidade(new BigDecimal("150.00"));
        a.setStatus(status);
        a.setDataMatricula(LocalDate.of(2000, 1, 1));
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

    @Test
    void vencimentoEhGravadoEAtrasoCalculado() {
        AlunoModel aluno = novoAluno("Aluno Vencimento", StatusAluno.ATIVO);
        AlunoModel dados = alunoService.buscarPorId(aluno.getId());
        dados.setDiaVencimento(5);
        alunoService.atualizar(aluno.getId(), dados);

        PagamentoModel p = pagamentoDe(pagamentoService.gerarMensalidades(MES, ANO), aluno);
        assertThat(p.getDataVencimento()).isEqualTo(LocalDate.of(ANO, MES, 5));

        PagamentoResponse emDia = PagamentoResponse.de(p, LocalDate.of(ANO, MES, 5));
        assertThat(emDia.atrasado()).isFalse();

        PagamentoResponse atrasado = PagamentoResponse.de(p, LocalDate.of(ANO, MES, 12));
        assertThat(atrasado.atrasado()).isTrue();
        assertThat(atrasado.diasAtraso()).isEqualTo(7);

        pagamentoService.registrarComprovante(p.getId());
        assertThat(PagamentoResponse.de(pagamentoService.buscarPorId(p.getId()),
                LocalDate.of(ANO, MES, 12)).atrasado()).isFalse();
    }

    @Test
    void trocarDiaDeVencimentoAtualizaSoPendentes() {
        AlunoModel aluno = novoAluno("Aluno Troca Dia", StatusAluno.ATIVO);
        PagamentoModel comComprovante = pagamentoDe(pagamentoService.gerarMensalidades(MES, ANO), aluno);
        pagamentoService.registrarComprovante(comComprovante.getId());
        PagamentoModel pendente = pagamentoDe(pagamentoService.gerarMensalidades(MES + 1, ANO), aluno);

        AlunoModel dados = alunoService.buscarPorId(aluno.getId());
        dados.setDiaVencimento(25);
        alunoService.atualizar(aluno.getId(), dados);

        assertThat(pagamentoService.buscarPorId(comComprovante.getId()).getDataVencimento())
            .isEqualTo(LocalDate.of(ANO, MES, 10));
        assertThat(pagamentoService.buscarPorId(pendente.getId()).getDataVencimento())
            .isEqualTo(LocalDate.of(ANO, MES + 1, 25));
    }

    @Test
    void naoCobraMesesAntesDaMatricula() {
        AlunoModel aluno = novoAluno("Aluno Novo", StatusAluno.ATIVO);
        AlunoModel dados = alunoService.buscarPorId(aluno.getId());
        dados.setDataMatricula(LocalDate.of(ANO, 3, 15));
        alunoService.atualizar(aluno.getId(), dados);

        assertThat(pagamentoService.gerarMensalidades(2, ANO))
            .noneMatch(p -> p.getAluno().getId().equals(aluno.getId()));
        assertThat(pagamentoService.gerarMensalidades(3, ANO))
            .anyMatch(p -> p.getAluno().getId().equals(aluno.getId()));
    }

    @Test
    void desfazerComprovanteVoltaParaPendente() {
        AlunoModel aluno = novoAluno("Aluno Engano", StatusAluno.ATIVO);
        PagamentoModel p = pagamentoDe(pagamentoService.gerarMensalidades(MES, ANO), aluno);

        assertThatThrownBy(() -> pagamentoService.desfazerComprovante(p.getId()))
            .isInstanceOf(IllegalStateException.class);

        pagamentoService.registrarComprovante(p.getId());
        PagamentoModel desfeito = pagamentoService.desfazerComprovante(p.getId());
        assertThat(desfeito.getStatusPagamento()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(desfeito.getComprovanteRecebido()).isFalse();

        // Com o comprovante desfeito, o aluno volta a poder ser excluído.
        alunoService.excluir(aluno.getId());
    }

    @Test
    void historicoDoAlunoVemDoMaisRecenteParaOMaisAntigo() {
        AlunoModel aluno = novoAluno("Aluno Historico", StatusAluno.ATIVO);
        pagamentoService.gerarMensalidades(MES, ANO);
        pagamentoService.gerarMensalidades(MES + 2, ANO);
        pagamentoService.gerarMensalidades(MES + 1, ANO);

        assertThat(pagamentoService.historicoDoAluno(aluno.getId()))
            .extracting(PagamentoModel::getMesReferencia)
            .containsExactly(MES + 2, MES + 1, MES);
    }

    @Test
    void graduacaoAtualizaFaixaAtual() {
        AlunoModel aluno = novoAluno("Aluno Faixa", StatusAluno.ATIVO);

        graduacaoService.registrar(aluno.getId(), new GraduacaoRequest("Azul", LocalDate.of(2020, 6, 1), null));
        var roxa = graduacaoService.registrar(aluno.getId(), new GraduacaoRequest("Roxa", LocalDate.of(2023, 6, 1), "Exame"));
        // Uma graduação antiga lançada depois não troca a faixa atual.
        graduacaoService.registrar(aluno.getId(), new GraduacaoRequest("Cinza", LocalDate.of(2018, 1, 1), null));

        assertThat(alunoService.buscarPorId(aluno.getId()).getFaixaAtual()).isEqualTo("Roxa");
        assertThat(graduacaoService.listar(aluno.getId()))
            .extracting(g -> g.getFaixa()).containsExactly("Roxa", "Azul", "Cinza");

        graduacaoService.excluir(aluno.getId(), roxa.getId());
        assertThat(alunoService.buscarPorId(aluno.getId()).getFaixaAtual()).isEqualTo("Azul");

        // Excluir o aluno leva as graduações junto.
        alunoService.excluir(aluno.getId());
    }

    @Test
    void relatorioDeReceitaSomaPorStatus() {
        List<ReceitaMensal> meses = pagamentoService.receitaMensal(6);

        assertThat(meses).hasSize(6);
        ReceitaMensal ultimo = meses.get(5);
        YearMonth atual = YearMonth.from(pagamentoService.hoje());
        assertThat(ultimo.mes()).isEqualTo(atual.getMonthValue());
        assertThat(ultimo.ano()).isEqualTo(atual.getYear());
        meses.forEach(m -> assertThat(m.recebido().add(m.aguardando()).add(m.pendente()))
            .isEqualByComparingTo(m.previsto()));

        assertThatThrownBy(() -> pagamentoService.receitaMensal(0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void alunoNovoNaoNasceEmAtrasoNoMesDaMatricula() {
        AlunoModel aluno = novoAluno("Aluno Recem Chegado", StatusAluno.ATIVO);
        AlunoModel dados = alunoService.buscarPorId(aluno.getId());
        dados.setDataMatricula(LocalDate.of(ANO, 3, 18));
        dados.setDiaVencimento(15);
        alunoService.atualizar(aluno.getId(), dados);

        // Mês da matrícula: vence na própria matrícula (18/03), não em 15/03.
        PagamentoModel marco = pagamentoDe(pagamentoService.gerarMensalidades(3, ANO), aluno);
        assertThat(marco.getDataVencimento()).isEqualTo(LocalDate.of(ANO, 3, 18));
        assertThat(PagamentoResponse.de(marco, LocalDate.of(ANO, 3, 18)).atrasado()).isFalse();

        // Meses seguintes: dia de vencimento normal.
        PagamentoModel abril = pagamentoDe(pagamentoService.gerarMensalidades(4, ANO), aluno);
        assertThat(abril.getDataVencimento()).isEqualTo(LocalDate.of(ANO, 4, 15));
    }
}
