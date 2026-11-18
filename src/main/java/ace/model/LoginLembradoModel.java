package ace.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Token do "Lembrar de mim", guardado no banco para sobreviver aos reinícios do servidor. */
@Entity
@Table(name = "login_lembrado")
public class LoginLembradoModel {

    @Id
    @Column(length = 64)
    private String serie;

    @Column(nullable = false, length = 150)
    private String usuario;

    @Column(nullable = false, length = 64)
    private String token;

    @Column(name = "ultimo_uso", nullable = false)
    private Instant ultimoUso;

    public LoginLembradoModel() {
    }

    public LoginLembradoModel(String serie, String usuario, String token, Instant ultimoUso) {
        this.serie = serie;
        this.usuario = usuario;
        this.token = token;
        this.ultimoUso = ultimoUso;
    }

    public String getSerie() {
        return serie;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Instant getUltimoUso() {
        return ultimoUso;
    }

    public void setUltimoUso(Instant ultimoUso) {
        this.ultimoUso = ultimoUso;
    }
}
