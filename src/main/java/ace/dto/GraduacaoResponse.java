package ace.dto;

import ace.model.GraduacaoModel;

import java.time.LocalDate;
import java.util.List;

public record GraduacaoResponse(Long id, String faixa, LocalDate data, String observacao) {

    public static GraduacaoResponse de(GraduacaoModel g) {
        return new GraduacaoResponse(g.getId(), g.getFaixa(), g.getData(), g.getObservacao());
    }

    public static List<GraduacaoResponse> de(List<GraduacaoModel> lista) {
        return lista.stream().map(GraduacaoResponse::de).toList();
    }
}
