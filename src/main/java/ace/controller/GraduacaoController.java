package ace.controller;

import ace.dto.GraduacaoRequest;
import ace.dto.GraduacaoResponse;
import ace.service.GraduacaoService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alunos/{idAluno}/graduacoes")
public class GraduacaoController {

    private final GraduacaoService graduacaoService;

    public GraduacaoController(GraduacaoService graduacaoService) {
        this.graduacaoService = graduacaoService;
    }

    @GetMapping
    public ResponseEntity<List<GraduacaoResponse>> listar(@PathVariable Long idAluno) {
        return ResponseEntity.ok(GraduacaoResponse.de(graduacaoService.listar(idAluno)));
    }

    @PostMapping
    public ResponseEntity<GraduacaoResponse> registrar(
            @PathVariable Long idAluno,
            @Valid @RequestBody GraduacaoRequest dados) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GraduacaoResponse.de(graduacaoService.registrar(idAluno, dados)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long idAluno, @PathVariable Long id) {
        graduacaoService.excluir(idAluno, id);
        return ResponseEntity.noContent().build();
    }
}
