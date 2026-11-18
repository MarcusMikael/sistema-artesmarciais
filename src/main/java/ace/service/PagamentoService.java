package ace.service;

import ace.dto.ReceitaMensal;
import ace.model.AlunoModel;
import ace.model.PagamentoModel;
import ace.model.enums.StatusAluno;
import ace.model.enums.StatusPagamento;
import ace.repository.AlunoRepository;
import ace.repository.PagamentoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final AlunoRepository alunoRepository;
    private final Clock clock;

    public PagamentoService(
            PagamentoRepository pagamentoRepository,
            AlunoRepository alunoRepository,
            Clock clock) {
        this.pagamentoRepository = pagamentoRepository;
        this.alunoRepository = alunoRepository;
        this.clock = clock;
    }

    /** Data de hoje no fuso da academia. */
    public LocalDate hoje() {
        return LocalDate.now(clock);
    }

    /** Dia de vencimento dentro do mês de referência (o dia é limitado a 28 no cadastro). */
    public static LocalDate calcularVencimento(int mes, int ano, int dia) {
        YearMonth ref = YearMonth.of(ano, mes);
        return ref.atDay(Math.min(dia, ref.lengthOfMonth()));
    }

    /** Vencimento gravado ou, em registros antigos, calculado pelo dia do aluno. */
    public static LocalDate vencimentoDe(PagamentoModel p) {
        if (p.getDataVencimento() != null) {
            return p.getDataVencimento();
        }
        return calcularVencimento(p.getMesReferencia(), p.getAnoReferencia(),
                p.getAluno().diaVencimentoEfetivo());
    }

    /** Valor gravado ou, em registros antigos, a mensalidade atual do aluno. */
    public static BigDecimal valorDe(PagamentoModel p) {
        if (p.getValor() != null) {
            return p.getValor();
        }
        BigDecimal mensalidade = p.getAluno().getValorMensalidade();
        return mensalidade != null ? mensalidade : BigDecimal.ZERO;
    }

    /** Pendências de alunos inativos não contam como cobrança do mês. */
    public static boolean contaNoMes(PagamentoModel p) {
        return p.getAluno().getStatus() == StatusAluno.ATIVO
                || p.getStatusPagamento() != StatusPagamento.PENDENTE;
    }

    @Transactional(readOnly = true)
    public List<PagamentoModel> listarTodos() {
        return pagamentoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public PagamentoModel buscarPorId(Long id) {
        return pagamentoRepository.findById(id)
                .orElseThrow(() ->
                    new NoSuchElementException("Pagamento não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<PagamentoModel> listarPorMesAno(Integer mes, Integer ano) {
        return pagamentoRepository
                .findByMesReferenciaAndAnoReferencia(mes, ano);
    }

    @Transactional(readOnly = true)
    public List<PagamentoModel> listarPorStatus(StatusPagamento status) {
        return pagamentoRepository.findByStatusPagamento(status);
    }

    @Transactional(readOnly = true)
    public List<PagamentoModel> historicoDoAluno(Long idAluno) {
        if (!alunoRepository.existsById(idAluno)) {
            throw new NoSuchElementException("Aluno não encontrado.");
        }
        return pagamentoRepository
                .findByAluno_IdOrderByAnoReferenciaDescMesReferenciaDesc(idAluno);
    }

    @Transactional
    public PagamentoModel criarPagamento(Long idAluno, Integer mes, Integer ano) {

        validarReferencia(mes, ano);

        AlunoModel aluno = alunoRepository.findById(idAluno)
                .orElseThrow(() ->
                    new NoSuchElementException("Aluno não encontrado."));

        if (aluno.getStatus() != StatusAluno.ATIVO) {
            throw new IllegalStateException(
                "Não é possível gerar pagamento para aluno inativo."
            );
        }

        boolean jaExiste = pagamentoRepository
                .existsByAluno_IdAndMesReferenciaAndAnoReferencia(
                    idAluno, mes, ano
                );

        if (jaExiste) {
            throw new IllegalStateException(
                "Já existe um pagamento para este aluno nesse mês."
            );
        }

        return pagamentoRepository.save(novaMensalidade(aluno, mes, ano));
    }

    @Transactional
    public List<PagamentoModel> gerarMensalidades(Integer mes, Integer ano) {
        validarReferencia(mes, ano);

        YearMonth referencia = YearMonth.of(ano, mes);

        for (AlunoModel aluno : alunoRepository.findByStatus(StatusAluno.ATIVO)) {
            // Não cobra meses anteriores à matrícula.
            if (aluno.getDataMatricula() != null
                    && YearMonth.from(aluno.getDataMatricula()).isAfter(referencia)) {
                continue;
            }

            boolean jaExiste = pagamentoRepository
                    .existsByAluno_IdAndMesReferenciaAndAnoReferencia(
                        aluno.getId(), mes, ano
                    );

            if (!jaExiste) {
                pagamentoRepository.save(novaMensalidade(aluno, mes, ano));
            }
        }

        return pagamentoRepository.findByMesReferenciaAndAnoReferencia(mes, ano);
    }

    private PagamentoModel novaMensalidade(AlunoModel aluno, int mes, int ano) {
        PagamentoModel pagamento = new PagamentoModel();
        pagamento.setAluno(aluno);
        pagamento.setMesReferencia(mes);
        pagamento.setAnoReferencia(ano);
        pagamento.setStatusPagamento(StatusPagamento.PENDENTE);
        pagamento.setComprovanteRecebido(false);
        pagamento.setValor(aluno.getValorMensalidade());
        pagamento.setDataVencimento(
                calcularVencimento(mes, ano, aluno.diaVencimentoEfetivo()));
        return pagamento;
    }

    private void validarReferencia(Integer mes, Integer ano) {
        if (mes == null || mes < 1 || mes > 12) {
            throw new IllegalArgumentException("Mês inválido.");
        }

        if (ano == null || ano < 2000 || ano > 2999) {
            throw new IllegalArgumentException("Ano inválido.");
        }
    }

    @Transactional
    public PagamentoModel registrarComprovante(Long id) {
        PagamentoModel pagamento = buscarPorId(id);

        if (pagamento.getStatusPagamento() == StatusPagamento.PAGO) {
            throw new IllegalStateException(
                "Este pagamento já foi confirmado."
            );
        }

        // Registros antigos sem valor gravado: congela o valor no recebimento.
        if (pagamento.getValor() == null) {
            pagamento.setValor(pagamento.getAluno().getValorMensalidade());
        }

        pagamento.setComprovanteRecebido(true);
        pagamento.setStatusPagamento(
            StatusPagamento.AGUARDANDO_CONFIRMACAO
        );

        return pagamentoRepository.save(pagamento);
    }

    /** Volta um comprovante registrado por engano para pendente. */
    @Transactional
    public PagamentoModel desfazerComprovante(Long id) {
        PagamentoModel pagamento = buscarPorId(id);

        if (pagamento.getStatusPagamento() != StatusPagamento.AGUARDANDO_CONFIRMACAO) {
            throw new IllegalStateException(
                "Somente comprovantes aguardando confirmação podem ser desfeitos."
            );
        }

        pagamento.setComprovanteRecebido(false);
        pagamento.setStatusPagamento(StatusPagamento.PENDENTE);
        // Volta a acompanhar o valor atual do aluno, como qualquer pendência.
        pagamento.setValor(pagamento.getAluno().getValorMensalidade());

        return pagamentoRepository.save(pagamento);
    }

    @Transactional
    public PagamentoModel confirmarPagamento(Long id) {
        PagamentoModel pagamento = buscarPorId(id);

        if (pagamento.getStatusPagamento()
                != StatusPagamento.AGUARDANDO_CONFIRMACAO
                || !Boolean.TRUE.equals(
                    pagamento.getComprovanteRecebido())) {

            throw new IllegalStateException(
                "O pagamento precisa ter um comprovante recebido " +
                "antes da confirmação."
            );
        }

        pagamento.setStatusPagamento(StatusPagamento.PAGO);
        pagamento.setDataHoraConfirmacao(LocalDateTime.now(clock));

        return pagamentoRepository.save(pagamento);
    }

    @Transactional
    public PagamentoModel desfazerConfirmacao(Long id) {
        PagamentoModel pagamento = buscarPorId(id);

        if (pagamento.getStatusPagamento() != StatusPagamento.PAGO) {
            throw new IllegalStateException(
                "Somente pagamentos confirmados podem ser desfeitos."
            );
        }

        pagamento.setStatusPagamento(
            StatusPagamento.AGUARDANDO_CONFIRMACAO
        );
        pagamento.setDataHoraConfirmacao(null);

        return pagamentoRepository.save(pagamento);
    }

    /** Receita dos últimos {@code meses} meses, terminando no mês atual (mais antigo primeiro). */
    @Transactional(readOnly = true)
    public List<ReceitaMensal> receitaMensal(int meses) {
        if (meses < 1 || meses > 36) {
            throw new IllegalArgumentException("Escolha entre 1 e 36 meses.");
        }

        YearMonth fim = YearMonth.now(clock);
        YearMonth inicio = fim.minusMonths(meses - 1L);
        List<PagamentoModel> pagamentos = pagamentoRepository.findEntreReferencias(
                inicio.getYear() * 100 + inicio.getMonthValue(),
                fim.getYear() * 100 + fim.getMonthValue());

        List<ReceitaMensal> resultado = new ArrayList<>();
        for (YearMonth ref = inicio; !ref.isAfter(fim); ref = ref.plusMonths(1)) {
            BigDecimal previsto = BigDecimal.ZERO;
            BigDecimal recebido = BigDecimal.ZERO;
            BigDecimal aguardando = BigDecimal.ZERO;
            BigDecimal pendente = BigDecimal.ZERO;
            int quantidade = 0;
            int pagos = 0;

            for (PagamentoModel p : pagamentos) {
                if (p.getMesReferencia() != ref.getMonthValue()
                        || p.getAnoReferencia() != ref.getYear()
                        || !contaNoMes(p)) {
                    continue;
                }
                BigDecimal valor = valorDe(p);
                previsto = previsto.add(valor);
                quantidade++;
                switch (p.getStatusPagamento()) {
                    case PAGO -> { recebido = recebido.add(valor); pagos++; }
                    case AGUARDANDO_CONFIRMACAO -> aguardando = aguardando.add(valor);
                    case PENDENTE -> pendente = pendente.add(valor);
                }
            }

            resultado.add(new ReceitaMensal(ref.getMonthValue(), ref.getYear(),
                    previsto, recebido, aguardando, pendente, quantidade, pagos));
        }
        return resultado;
    }
}
