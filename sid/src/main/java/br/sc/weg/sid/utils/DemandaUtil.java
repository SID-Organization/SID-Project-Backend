package br.sc.weg.sid.utils;

import br.sc.weg.sid.DTO.CadastroDemandaDTO;
import br.sc.weg.sid.DTO.CadastroPdfDemandaDTO;
import br.sc.weg.sid.model.entities.Demanda;
import br.sc.weg.sid.model.entities.DemandaResumida;
import br.sc.weg.sid.model.entities.PdfDemanda;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
public class DemandaUtil {

    private ObjectMapper mapper = new ObjectMapper();

    public Demanda convertDtoToModel(CadastroDemandaDTO cadastroDemandaDTO) {
        Demanda demanda = new Demanda();
        BeanUtils.copyProperties(cadastroDemandaDTO, demanda);
        return demanda;
    }

    public CadastroDemandaDTO convertToDto(String demandaJson) {
        try {
            return this.mapper.readValue(demandaJson, CadastroDemandaDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao converter o demandaJson para objeto CadastroDemandaDTO! \n" + e.getMessage());
        }
    }

    public CadastroPdfDemandaDTO convertToPdfDto(String pdfDemandaJson) {
        try {
            System.out.println("PDFFFFFFFFFFFFFFF: " + pdfDemandaJson);
            return this.mapper.readValue(pdfDemandaJson, CadastroPdfDemandaDTO.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao converter o demandaJson para objeto CadastroPdfDemandaDTO! \n" + e.getMessage());
        }
    }

    public PdfDemanda convertPdfDtoToModel(CadastroPdfDemandaDTO cadastroPdfDemandaDTO) {
        PdfDemanda pdfDemanda = new PdfDemanda();
        BeanUtils.copyProperties(cadastroPdfDemandaDTO, pdfDemanda);
        return pdfDemanda;
    }

    public List<DemandaResumida> resumirDemanda(List<Demanda> demandas) {
        List<DemandaResumida> demandasResumidas = new ArrayList<>();
        for (Demanda demanda : demandas) {
            System.out.println("DEMANDA: " + demanda);
            DemandaResumida demandaResumida = new DemandaResumida();
            demandaResumida.setTituloDemanda(demanda.getTituloDemanda());
            demandaResumida.setStatusDemanda(demanda.getStatusDemanda());
            demandaResumida.setPrazoElaboracaoDemanda(demanda.getPrazoElaboracaoDemanda());
            demandaResumida.setScoreDemanda(demanda.getScoreDemanda());
            demandaResumida.setIdDemanda(demanda.getIdDemanda());
            demandaResumida.setNomeSolicitante(demanda.getSolicitanteDemanda().getNomeUsuario());
            demandaResumida.setNomeAnalistaResponsavel(demanda.getAnalistaResponsavelDemanda().getNomeUsuario());
            demandaResumida.setNomeGerenteResponsavelDemanda(demanda.getGerenteDaAreaDemanda().getNomeUsuario());
            demandaResumida.setMotivoRecusaDemanda(demanda.getMotivosRecusaDemanda().get(demanda.getMotivosRecusaDemanda().size() - 1).getDescricaoMotivoRecusa());

            if (demanda.getBuSolicitanteDemanda() != null && demanda.getBuSolicitanteDemanda().getNomeBusinessUnity() != null) {
                demandaResumida.setDepartamentoDemanda(demanda.getBuSolicitanteDemanda().getNomeBusinessUnity());
            } else {
                demandaResumida.setDepartamentoDemanda(null);
            }
            if (demanda.getTamanhoDemanda() != null) {
                demandaResumida.setTamanhoDemanda(demanda.getTamanhoDemanda().getNome());
            } else {
                demandaResumida.setTamanhoDemanda(null);
            }
            demandaResumida.setCodigoPPMDemanda(demanda.getCodigoPPMDemanda());
            demandasResumidas.add(demandaResumida);
        }
        return demandasResumidas;
    }

}
