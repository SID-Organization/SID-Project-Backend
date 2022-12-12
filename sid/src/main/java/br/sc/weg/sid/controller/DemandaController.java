package br.sc.weg.sid.controller;

import br.sc.weg.sid.DTO.CadastroBusBeneficiadasDemanda;
import br.sc.weg.sid.DTO.CadastroDemandaDTO;
import br.sc.weg.sid.DTO.CadastroHistoricoWorkflowDTO;
import br.sc.weg.sid.model.entities.*;
import br.sc.weg.sid.model.service.*;
import br.sc.weg.sid.utils.DemandaUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Controller
@CrossOrigin
@RequestMapping("/sid/api/demanda")
public class DemandaController {

    @Autowired
    HistoricoWorkflowController historicoWorkflowController;

    @Autowired
    DemandaService demandaService;

    @Autowired
    UsuarioService usuarioService;

    @Autowired
    BusinessUnityService businessUnityService;

    @Autowired
    ArquivoDemandaService arquivoDemandaService;

    @Autowired
    BeneficioService beneficioService;

    //Get all, pega todas as demandas
    @GetMapping()
    public ResponseEntity<Object> findAll() {
        List<Demanda> demandas = demandaService.findAll();
        if (demandas.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhuma demanda encontrada");
        }
        return ResponseEntity.status(HttpStatus.FOUND).body(demandas);
    }

    //Cria uma demanda(caso a demanda não tenha os campos totalmente preenchidos cadastrará com o status de RASCUNHO) e retorna a demanda criada
    @PostMapping()
    public ResponseEntity<Object> cadastroDemandas(

            @RequestParam("demandaForm") @Valid String demandaJson,
            @RequestParam(value = "arquivosDemanda", required = false) MultipartFile[] additionalImages
    ) {
        try {
            DemandaUtil demandaUtil = new DemandaUtil();
            CadastroDemandaDTO cadastroDemandaDTO = demandaUtil.convertToDto(demandaJson);
            Demanda demanda = demandaUtil.convertDtoToModel(cadastroDemandaDTO);
            try {
                demanda.setSolicitanteDemanda(usuarioService.findById(cadastroDemandaDTO.getSolicitanteDemanda().getNumeroCadastroUsuario()).get());
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Solicitante não encontrado!");
            }
            Usuario usuarioBusinesUnity = usuarioService.findById(demanda.getSolicitanteDemanda().getNumeroCadastroUsuario()).get();
            demanda.setScoreDemanda(549.00);
            demanda.setStatusDemanda(StatusDemanda.ABERTA);

            //Verifica se a demanda possui todos os campos preenchidos, se não possuir, o status será RASCUNHO
            Class<? extends CadastroDemandaDTO> classe = cadastroDemandaDTO.getClass();
            List<Field> atributos = Arrays.asList(classe.getDeclaredFields());
            atributos.forEach(atributo -> {
                try {
                    Object valor = atributo.get(cadastroDemandaDTO);
                    if (valor == null) {
                        demanda.setStatusDemanda(StatusDemanda.RASCUNHO);
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            });
            Demanda demandaSalva = demandaService.save(demanda);

            //Cadastra BU's beneficiadas e verifica se elas existem
//            List<BusBeneficiadasDemanda> busBeneficiadasDemandasList = new ArrayList<>();
//            for (int i =0; i < cadastroDemandaDTO.getBusBeneficiadas().size(); i++){
//                BusBeneficiadasDemanda busBeneficiadasDemanda = new BusBeneficiadasDemanda();
//                try {
//                    busBeneficiadasDemanda.setBusinessUnityBeneficiada(businessUnityService.findById(
//                            cadastroDemandaDTO.getBusBeneficiadas().get(i).getIdBusinessUnity()).get());
//                } catch (Exception e) {
//                    demandaService.deleteById(demandaSalva.getIdDemanda());
//                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("BU com id: " + cadastroDemandaDTO.getBusBeneficiadas().get(i).getIdBusinessUnity()
//                            + " não encontrada!");
//                }
//                busBeneficiadasDemanda.setDemandaBusBeneficiadas(demandaSalva);
//                busBeneficiadasDemandasList.add(busBeneficiadasDemanda);
//            }
//            demandaSalva.setBusBeneficiadas(busBeneficiadasDemandasList);
//            demandaService.updateBusBeneficiadasDemanda(demandaSalva.getIdDemanda(), bu);

            //essa variável tem como objetivo buscar a data do dia atual para ser inserida no arquivo de demanda
            LocalDate localDate = LocalDate.now();
            Date dataRegistroArquivo = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            ArquivoDemanda arquivoDemandaSalvo = new ArquivoDemanda();
            //Cadastra os arquivos da demanda
            if (additionalImages != null) {
                try {
                    for (MultipartFile additionalImage : additionalImages) {
                        ArquivoDemanda arquivoDemanda = new ArquivoDemanda();
                        arquivoDemanda.setNomeArquivo(additionalImage.getOriginalFilename());
                        arquivoDemanda.setTipoArquivo(additionalImage.getContentType());
                        arquivoDemanda.setArquivo(additionalImage.getBytes());
                        arquivoDemanda.setIdDemanda(demandaSalva);
                        arquivoDemanda.setIdUsuario(usuarioService.findById(cadastroDemandaDTO.getSolicitanteDemanda().getNumeroCadastroUsuario()).get());
                        arquivoDemanda.setDataRegistroArquivo(dataRegistroArquivo);
                        arquivoDemandaSalvo = arquivoDemandaService.save(arquivoDemanda);
                        demandaSalva.getArquivosDemandas().add(arquivoDemandaSalvo);
                    }
                } catch (Exception e) {
                    arquivoDemandaService.deleteById(arquivoDemandaSalvo.getIdArquivoDemanda());
                    demandaService.deleteById(demandaSalva.getIdDemanda());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erro ao salvar arquivos: " + e.getMessage());
                }
            }
            //Cadastra os benefícios da demanda
            for (Beneficio beneficio : demandaSalva.getBeneficiosDemanda()) {
                beneficio.setIdDemanda(demandaSalva);
                beneficioService.save(beneficio);
            }

            //Se a demanda tiver em status Aberta(Backlog) um historico de workflow é criado
            if (demandaSalva.getStatusDemanda().equals(StatusDemanda.ABERTA)) {
                CadastroHistoricoWorkflowDTO historicoWorkflowDTO = new CadastroHistoricoWorkflowDTO();
                historicoWorkflowDTO.setDemandaHistorico(demandaSalva);
                historicoWorkflowDTO.setIdResponsavel(demandaSalva.getSolicitanteDemanda());
                historicoWorkflowDTO.setTarefaHistoricoWorkflow(TarefaWorkflow.PREENCHER_DEMANDA);
                try {
                    historicoWorkflowController.cadastroHistoricoWorkflow(historicoWorkflowDTO);
                } catch (Exception e) {
                    demandaService.deleteById(demandaSalva.getIdDemanda());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erro ao salvar histórico de workflow: " + e.getMessage());
                }
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(demandaSalva);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Erro ao cadastrar demanda: " + e.getMessage());
        }
    }

    //Busca demanda por id
    @GetMapping("/id/{id}")
    public ResponseEntity<Object> findById(@PathVariable("id") Integer id) {
        try {
            Demanda demanda = demandaService.findById(id).get();
            return ResponseEntity.status(HttpStatus.FOUND).body(demanda);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Demanda com id: " + id + " não encontrada!");
        }
    }

    //Busca demandas por statusDemanda
    @GetMapping("/statusDemanda/{statusDemanda}")
    public ResponseEntity<Object> findByStatus(@PathVariable("statusDemanda") StatusDemanda statusDemanda) {
        try {
            List<Demanda> demandas = demandaService.findByStatusDemanda(statusDemanda);
            if (demandas.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhuma demanda com statusDemanda " + statusDemanda + " encontrada!");
            }
            return ResponseEntity.status(HttpStatus.FOUND).body(demandas);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhuma demanda com statusDemanda " + statusDemanda + " encontrada!");
        }
    }

    //Busca demanda por solicitante
    @GetMapping("/solicitante/{numeroCadastroSoliciante}")
    public ResponseEntity<Object> findBySolicitante(@PathVariable("numeroCadastroSoliciante") Integer numeroCadastroSoliciante) {
        try {
            Usuario solicitanteDemanda = usuarioService.findById(numeroCadastroSoliciante).get();
            List<Demanda> demandas = demandaService.findBySolicitanteDemanda(solicitanteDemanda);
            if (demandas.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("O solicitante " + solicitanteDemanda.getNomeUsuario() + " não possui demandas!");
            }
            return ResponseEntity.status(HttpStatus.FOUND).body(demandas);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Solicitante com matrícula: " + numeroCadastroSoliciante + " não encontrado!");
        }
    }

    //Busca demandas por Seção
    @GetMapping("/secao/{secao}")
    public ResponseEntity<Object> findBySecao(@PathVariable("secao") String secao) {
        try {
            List<Demanda> demandas = demandaService.findBySecaoTIResponsavel(secao);
            if (demandas.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhuma demanda na seção " + secao + " encontrada!");
            }
            return ResponseEntity.status(HttpStatus.FOUND).body(demandas);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Seção " + secao + " não existe!");
        }
    }

    //Busca demandas por data de criação (mais nova a mais antiga)
    @GetMapping("/data-decrescente")
    public ResponseEntity<Object> findByDataDecrescente() {
        List<Demanda> demandas = demandaService.findByPrazoElaboracaoDemandaDesc();
        if (demandas.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhuma demanda encontrada!");
        }
        return ResponseEntity.status(HttpStatus.FOUND).body(demandas);
    }

    //Busca demanda por data de criação (mais antiga a mais nova)
    @GetMapping("/data-crescente")
    public ResponseEntity<Object> findByDataCrescente() {
        List<Demanda> demandas = demandaService.findByPrazoElaboracaoDemandaAsc();
        if (demandas.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhuma demanda encontrada!");
        }
        return ResponseEntity.status(HttpStatus.FOUND).body(demandas);
    }

    //Busca demanda por score
    @GetMapping("/score/{score}")
    public ResponseEntity<Object> findByScore(@PathVariable("score") Double score) {
        List<Demanda> demandas = demandaService.findByScoreDemanda(score);
        if (demandas.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhuma demanda com score de: " + score + " foi encontrada!");
        }
        return ResponseEntity.status(HttpStatus.FOUND).body(demandas);
    }

    //Busca demandas pelo titulo
    @GetMapping("/titulo-demanda/{tituloDemanda}")
    public ResponseEntity<Object> findByTituloDemanda(@PathVariable("tituloDemanda") String tituloDemanda) {
        List<Demanda> demandas = demandaService.findByTituloDemanda(tituloDemanda);
        if (demandas.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhuma demanda com o título: " + tituloDemanda + " foi encontrada!");
        }
        return ResponseEntity.status(HttpStatus.FOUND).body(demandas);
    }

    //Busca demandas pelo tamanhoDemanda
    @GetMapping("/tamanhoDemanda/{tamanhoDemanda}")
    public ResponseEntity<Object> findByTamanho(@PathVariable("tamanhoDemanda") TamanhoDemanda tamanhoDemanda) {
        List<Demanda> demandas = demandaService.findByTamanhoDemanda(tamanhoDemanda);
        if (demandas.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhuma demanda com o tamanhoDemanda: " + tamanhoDemanda + " foi encontrada!");
        }
        return ResponseEntity.status(HttpStatus.FOUND).body(demandas);
    }

    //Atualiza uma demanda informando seu id
    @PutMapping("/{id}")
    public ResponseEntity<Object> atualizarDemanda(
            @PathVariable("id") Integer id,
            @RequestParam("demandaForm") @Valid String demandaJson
    ) {
        DemandaUtil demandaUtil = new DemandaUtil();
        CadastroDemandaDTO cadastroDemandaDTO = demandaUtil.convertToDto(demandaJson);
        if (!demandaService.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Não foi encontrado a demanda com o id " + id);
        }
        Demanda demanda = demandaService.findById(id).get();
        BeanUtils.copyProperties(cadastroDemandaDTO, demanda);
        historicoWorkflowController.atualizaVersao(demanda.getHistoricoWorkflowUltimaVersao().getIdHistoricoWorkflow(),
                demanda.getHistoricoWorkflowUltimaVersao(), demanda);
        demandaService.save(demanda);
        return ResponseEntity.status(HttpStatus.OK).body(demanda);
    }

    @PutMapping("/atualiza-bus-beneficiadas/{id}")
    public ResponseEntity<Object> atualizaBusBeneficiadas(
            @PathVariable("id") Integer id,
            @RequestBody @Valid CadastroBusBeneficiadasDemanda cadastroBusBeneficiadasDemanda
            ) {
        if (!demandaService.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Não foi encontrado a demanda com o id " + id);
        }
        Demanda demanda = demandaService.findById(id).get();
        demanda.setSecaoTIResponsavel(cadastroBusBeneficiadasDemanda.getSecaoTIResponsavel());
        demanda.setBuSolicitanteDemanda(cadastroBusBeneficiadasDemanda.getBuSolicitante());
        demanda.setBusBeneficiadasDemanda(cadastroBusBeneficiadasDemanda.getBusBeneficiadasDemanda());
        demanda.setTamanhoDemanda(cadastroBusBeneficiadasDemanda.getTamanhoDemanda());
        demanda.setStatusDemanda(StatusDemanda.CLASSIFICADO_PELO_ANALISTA);
        demandaService.save(demanda);
        return ResponseEntity.status(HttpStatus.OK).body(demanda);
    }

    //Deleta uma demanda informando seu id
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deletarDemanda(@PathVariable("id") Integer id) {
        try {
            if (!demandaService.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Não foi encontrado a demanda com o id " + id);
            }
            if (demandaService.findById(id).get().getStatusDemanda().equals(StatusDemanda.RASCUNHO)) {
                demandaService.deleteById(id);
                return ResponseEntity.status(HttpStatus.OK).body("Demanda com o id: " + id + " deletada com sucesso!");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Demanda com o id: " + id + " não pode ser deletada pois não tem o status rascunho!");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erro ao deletar a demanda: " + e.getMessage());
        }
    }
}
