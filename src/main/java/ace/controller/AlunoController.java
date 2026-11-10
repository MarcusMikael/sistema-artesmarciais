package ace.controller;

import ace.dto.AlunoRequest;
import ace.dto.AlunoResponse;
import ace.model.AlunoModel;
import ace.model.enums.StatusAluno;
import ace.service.AlunoService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alunos")
public class AlunoController {

    private final AlunoService alunoService;

    public AlunoController(AlunoService alunoService) {
        this.alunoService = alunoService;
    }

    @GetMapping
    public ResponseEntity<List<AlunoResponse>> listarTodos() {
        return ResponseEntity.ok(AlunoResponse.de(alunoService.listarTodos()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlunoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(AlunoResponse.de(alunoService.buscarPorId(id)));
    }

    @PostMapping
    public ResponseEntity<AlunoResponse> cadastrar(@Valid @RequestBody AlunoRequest dto) {
        AlunoModel novoAluno = alunoService.cadastrar(dto.paraModel());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AlunoResponse.de(novoAluno));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlunoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody AlunoRequest dto) {

        AlunoModel atualizado = alunoService.atualizar(id, dto.paraModel());
        return ResponseEntity.ok(AlunoResponse.de(atualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        alunoService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<AlunoResponse>> buscarPorNome(@RequestParam String nome) {
        return ResponseEntity.ok(AlunoResponse.de(alunoService.buscarPorNome(nome)));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<AlunoResponse>> listarPorStatus(@PathVariable StatusAluno status) {
        return ResponseEntity.ok(AlunoResponse.de(alunoService.listarPorStatus(status)));
    }
}
