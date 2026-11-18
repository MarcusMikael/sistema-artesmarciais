package ace.repository;

import ace.model.GraduacaoModel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GraduacaoRepository extends JpaRepository<GraduacaoModel, Long> {

    // Mais recente primeiro.
    List<GraduacaoModel> findByAluno_IdOrderByDataDescIdDesc(Long idAluno);

    Optional<GraduacaoModel> findFirstByAluno_IdOrderByDataDescIdDesc(Long idAluno);

    Optional<GraduacaoModel> findByIdAndAluno_Id(Long id, Long idAluno);

    void deleteByAluno_Id(Long idAluno);
}
