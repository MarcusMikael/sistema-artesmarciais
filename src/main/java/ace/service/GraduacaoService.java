package ace.service;

import ace.dto.GraduacaoRequest;
import ace.model.AlunoModel;
import ace.model.GraduacaoModel;
import ace.repository.GraduacaoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class GraduacaoService {

    private final GraduacaoRepository graduacaoRepository;
    private final AlunoService alunoService;

    public GraduacaoService(GraduacaoRepository graduacaoRepository, AlunoService alunoService) {
        this.graduacaoRepository = graduacaoRepository;
        this.alunoService = alunoService;
    }

    @Transactional(readOnly = true)
    public List<GraduacaoModel> listar(Long idAluno) {
        alunoService.buscarPorId(idAluno);
        return graduacaoRepository.findByAluno_IdOrderByDataDescIdDesc(idAluno);
    }

    /** Registra a graduação; a faixa atual do aluno passa a ser a da graduação mais recente. */
    @Transactional
    public GraduacaoModel registrar(Long idAluno, GraduacaoRequest dados) {
        AlunoModel aluno = alunoService.buscarPorId(idAluno);

        GraduacaoModel graduacao = new GraduacaoModel();
        graduacao.setAluno(aluno);
        graduacao.setFaixa(dados.faixa().trim());
        graduacao.setData(dados.data());
        graduacao.setObservacao(dados.observacao() == null || dados.observacao().isBlank()
                ? null : dados.observacao().trim());
        GraduacaoModel salva = graduacaoRepository.saveAndFlush(graduacao);

        atualizarFaixaAtual(aluno);
        return salva;
    }

    @Transactional
    public void excluir(Long idAluno, Long idGraduacao) {
        GraduacaoModel graduacao = graduacaoRepository.findByIdAndAluno_Id(idGraduacao, idAluno)
                .orElseThrow(() -> new NoSuchElementException("Graduação não encontrada."));
        AlunoModel aluno = graduacao.getAluno();

        graduacaoRepository.delete(graduacao);
        graduacaoRepository.flush();
        atualizarFaixaAtual(aluno);
    }

    private void atualizarFaixaAtual(AlunoModel aluno) {
        graduacaoRepository.findFirstByAluno_IdOrderByDataDescIdDesc(aluno.getId())
                .ifPresent(ultima -> aluno.setFaixaAtual(ultima.getFaixa()));
    }
}
