package ace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record GraduacaoRequest(

        @NotBlank(message = "Informe a faixa.")
        @Size(max = 50, message = "Faixa muito longa.")
        String faixa,

        @NotNull(message = "Informe a data da graduação.")
        @PastOrPresent(message = "A data não pode ser no futuro.")
        LocalDate data,

        @Size(max = 255, message = "Observação muito longa.")
        String observacao
) {
}
