package br.sc.weg.sid.DTO;

import br.sc.weg.sid.model.entities.*;
import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

import javax.persistence.*;
import javax.validation.constraints.FutureOrPresent;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class CadastroDemandaDTO {

    private String tituloDemanda;

    private String objetivoDemanda;

//    private String sessaoTIResponsavel;

    private String situacaoAtualDemanda;

    private Integer frequenciaUsoDemanda;

    private String descricaoQualitativoDemanda;

    @FutureOrPresent
    private Date prazoElaboracaoDemanda;

    private BusinessUnity idBuSolicitante;

    private Usuario idUsuario;

    private Chat idChat;

//    private Proposta idProposta;

    private List<BusinessUnity> busBeneficiadas;

    private List<Beneficio> beneficios;
}
