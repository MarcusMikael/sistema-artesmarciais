package ace.repository;

import ace.model.PagamentoModel;
import ace.model.enums.StatusPagamento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagamentoRepository extends JpaRepository<PagamentoModel, Long> {

    // Buscar pagamentos de um aluno
    List<PagamentoModel> findByAluno_Id(Long idAluno);

    // Buscar pagamentos por mês e ano
    List<PagamentoModel> findByMesReferenciaAndAnoReferencia(
            Integer mes, Integer ano
    );

    // Buscar pagamentos por status
    List<PagamentoModel> findByStatusPagamento(StatusPagamento status);

    // Buscar pagamentos de um mês e ano por status
    List<PagamentoModel> findByMesReferenciaAndAnoReferenciaAndStatusPagamento(
            Integer mes,
            Integer ano,
            StatusPagamento status
    );

    // Verificar se já existe pagamento para o aluno naquele mês
    boolean existsByAluno_IdAndMesReferenciaAndAnoReferencia(
            Long idAluno,
            Integer mes,
            Integer ano
    );

    boolean existsByAluno_Id(Long idAluno);
}