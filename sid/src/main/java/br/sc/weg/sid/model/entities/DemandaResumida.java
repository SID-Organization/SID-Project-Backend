package br.sc.weg.sid.model.entities;

import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
public class DemandaResumida {

    Integer idDemanda;
    String tituloDemanda;

    Date prazoElaboracaoDemanda;

    Double scoreDemanda;

    StatusDemanda statusDemanda;

    String nomeSolicitante;

    String nomeAnalistaResponsavel;

    String nomeGerenteResponsavelDemanda;

    String forumDeAprovacaoDemanda;

    String departamentoDemanda;

    String tamanhoDemanda;

    Integer codigoPPMDemanda;
}
