package ace.repository;

import ace.model.AlunoModel;
import ace.model.enums.StatusAluno;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlunoRepository extends JpaRepository<AlunoModel, Long> {

    // Buscar alunos pelo nome
    List<AlunoModel> findByNomeContainingIgnoreCase(String nome);

    // Buscar alunos pelo status
    List<AlunoModel> findByStatus(StatusAluno status);

    // Buscar alunos pelo nome e status
    List<AlunoModel> findByNomeContainingIgnoreCaseAndStatus(
            String nome, StatusAluno status
    );
}