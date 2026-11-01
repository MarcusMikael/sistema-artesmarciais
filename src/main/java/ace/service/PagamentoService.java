
package ace.service;

import ace.model.AlunoModel;
import ace.model.PagamentoModel;
import ace.model.enums.StatusAluno;
import ace.model.enums.StatusPagamento;
import ace.repository.AlunoRepository;
import ace.repository.PagamentoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final AlunoRepository alunoRepository;

    public PagamentoService(
            PagamentoRepository pagamentoRepository,
            AlunoRepository alunoRepository) {
        this.pagamentoRepository = pagamentoRepository;
        this.alunoRepository = alunoRepository;
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

    @Transactional
    public PagamentoModel criarPagamento(Long idAluno, Integer mes, Integer ano) {

        if (mes == null || mes < 1 || mes > 12) {
            throw new IllegalArgumentException("Mês inválido.");
        }

        if (ano == null || ano < 2000) {
            throw new IllegalArgumentException("Ano inválido.");
        }

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

        PagamentoModel pagamento = new PagamentoModel();

        pagamento.setAluno(aluno);
        pagamento.setMesReferencia(mes);
        pagamento.setAnoReferencia(ano);
        pagamento.setStatusPagamento(StatusPagamento.PENDENTE);
        pagamento.setComprovanteRecebido(false);

        return pagamentoRepository.save(pagamento);
    }

    @Transactional
    public PagamentoModel registrarComprovante(Long id) {
        PagamentoModel pagamento = buscarPorId(id);

        if (pagamento.getStatusPagamento() == StatusPagamento.PAGO) {
            throw new IllegalStateException(
                "Este pagamento já foi confirmado."
            );
        }

        pagamento.setComprovanteRecebido(true);
        pagamento.setStatusPagamento(
            StatusPagamento.AGUARDANDO_CONFIRMACAO
        );

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
        pagamento.setDataHoraConfirmacao(LocalDateTime.now());

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
}

