package ace.repository;

import ace.model.LoginLembradoModel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginLembradoRepository extends JpaRepository<LoginLembradoModel, String> {

    void deleteByUsuario(String usuario);

    long countByUsuario(String usuario);
}
