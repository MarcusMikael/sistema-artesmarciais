
package ace.controller;

import ace.model.AlunoModel;
import ace.model.enums.StatusAluno;
import ace.service.AlunoService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alunos")
@CrossOrigin(origins = "*")
public class AlunoController {

    private final AlunoService alunoService;

    public AlunoController(AlunoService alunoService) {
        this.alunoService = alunoService;
    }

    @GetMapping
    public List<AlunoModel> listarTodos() {
        return alunoService.listarTodos();
    }

    @GetMapping("/{id}")
    public AlunoModel buscarPorId(@PathVariable Long id) {
        return alunoService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<AlunoModel> cadastrar(@RequestBody AlunoModel aluno) {
        AlunoModel novoAluno = alunoService.cadastrar(aluno);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(novoAluno);
    }

    @PutMapping("/{id}")
    public AlunoModel atualizar(
            @PathVariable Long id,
            @RequestBody AlunoModel aluno) {

        return alunoService.atualizar(id, aluno);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        alunoService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buscar")
    public List<AlunoModel> buscarPorNome(@RequestParam String nome) {
        return alunoService.buscarPorNome(nome);
    }

    @GetMapping("/status/{status}")
    public List<AlunoModel> listarPorStatus(@PathVariable StatusAluno status) {
        return alunoService.listarPorStatus(status);
    }
}

