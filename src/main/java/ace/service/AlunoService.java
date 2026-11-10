
package ace.service;

import ace.model.AlunoModel;
import ace.model.enums.StatusAluno;
import ace.model.enums.StatusPagamento;
import ace.repository.AlunoRepository;
import ace.repository.PagamentoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class AlunoService {

    private final AlunoRepository alunoRepository;
    private final PagamentoRepository pagamentoRepository;

    public AlunoService(
            AlunoRepository alunoRepository,
            PagamentoRepository pagamentoRepository) {
        this.alunoRepository = alunoRepository;
        this.pagamentoRepository = pagamentoRepository;
    }

    @Transactional(readOnly = true)
    public List<AlunoModel> listarTodos() {
        return alunoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public AlunoModel buscarPorId(Long id) {
        return alunoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Aluno não encontrado."));
    }

    @Transactional
    public AlunoModel cadastrar(AlunoModel aluno) {
        if (aluno.getStatus() == null) {
            aluno.setStatus(StatusAluno.ATIVO);
        }

        return alunoRepository.save(aluno);
    }

    @Transactional
    public AlunoModel atualizar(Long id, AlunoModel dados) {
        AlunoModel aluno = buscarPorId(id);

        aluno.setNome(dados.getNome());
        aluno.setTelefone(dados.getTelefone());
        aluno.setModalidade(dados.getModalidade());
        aluno.setTurmaHorario(dados.getTurmaHorario());
        aluno.setFaixaAtual(dados.getFaixaAtual());
        aluno.setValorMensalidade(dados.getValorMensalidade());

        if (dados.getStatus() != null) {
            aluno.setStatus(dados.getStatus());
        }

        // Reajuste/correção do valor vale para as mensalidades ainda pendentes.
        // Pagamentos com comprovante ou confirmados mantêm o valor original.
        pagamentoRepository
                .findByAluno_IdAndStatusPagamento(id, StatusPagamento.PENDENTE)
                .forEach(p -> p.setValor(aluno.getValorMensalidade()));

        return alunoRepository.save(aluno);
    }

    @Transactional
    public void excluir(Long id) {
        AlunoModel aluno = buscarPorId(id);

        // Mensalidades apenas geradas (PENDENTE, sem comprovante) não contam
        // como histórico financeiro e são removidas junto com o aluno.
        if (pagamentoRepository.existsByAluno_IdAndStatusPagamentoNot(
                id, StatusPagamento.PENDENTE)) {
            throw new IllegalStateException(
                    "Este aluno possui histórico financeiro. " +
                            "Altere seu status para Inativo em vez de excluí-lo.");
        }

        pagamentoRepository.deleteByAluno_Id(id);
        alunoRepository.delete(aluno);
    }

    @Transactional(readOnly = true)
    public List<AlunoModel> buscarPorNome(String nome) {
        return alunoRepository.findByNomeContainingIgnoreCase(nome);
    }

    @Transactional(readOnly = true)
    public List<AlunoModel> listarPorStatus(StatusAluno status) {
        return alunoRepository.findByStatus(status);
    }
}
