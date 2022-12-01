package br.sc.weg.sid.controller;

import br.sc.weg.sid.DTO.CadastroHistoricoWorkflowDTO;
import br.sc.weg.sid.model.entities.*;
import br.sc.weg.sid.model.service.DemandaService;
import br.sc.weg.sid.model.service.HistoricoWorkflowService;
import br.sc.weg.sid.utils.HistoricoWorkflowUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/sid/api/historico-workflow")
public class HistoricoWorkflowController {

    @Autowired
    private HistoricoWorkflowService historicoWorkflowService;

    @Autowired
    private DemandaService demandaService;

    @GetMapping("/teste")
    public void teste() {
        historicoWorkflowService.teste();
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping()
    public ResponseEntity<Object> findAll() {
        try {

            List<HistoricoWorkflow> historicoWorkflows = historicoWorkflowService.findAll();
            List<HistoricoWorkflowResumido> historicoWorkflowResumidos = HistoricoWorkflowUtil.converterHistoricoWorkflowParaHistoricoWorkflowReumido(historicoWorkflows);
            if (historicoWorkflows.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhum histórico de workflow encontrado!");
            }
            return ResponseEntity.status(HttpStatus.FOUND).body(historicoWorkflowResumidos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar histórico de workflow: " + e.getMessage());
        }
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @PostMapping()
    public ResponseEntity<Object> cadastroHistoricoWorkflow(
            @RequestBody CadastroHistoricoWorkflowDTO historicoWorkflowDTO
    ) {
        HistoricoWorkflowUtil historicoWorkflowUtil = new HistoricoWorkflowUtil();
        HistoricoWorkflow historicoWorkflow = new HistoricoWorkflow();
        BeanUtils.copyProperties(historicoWorkflowDTO, historicoWorkflow);
        if (historicoWorkflow.getTarefaHistoricoWorkflow() == TarefaWorkflow.PREENCHER_DEMANDA) {
            historicoWorkflow.setStatusWorkflow(StatusWorkflow.CONCLUIDO);
            historicoWorkflow.setVersaoHistorico(0.1);
            historicoWorkflow.setAcaoFeitaHistorico("Enviar");
        } else {
            Demanda demanda = demandaService.findById(historicoWorkflow.getDemandaHistorico().getIdDemanda()).get();
            HistoricoWorkflow historicoWorkflowAnterior = demanda.getHistoricoWorkflowUltimaVersao();
            if(historicoWorkflow.equals(historicoWorkflowAnterior)) {
                return ResponseEntity.status(HttpStatus.OK).body("Não houveram alterações!");
            }
            historicoWorkflow.setStatusWorkflow(StatusWorkflow.EM_ANDAMENTO);
        }
        LocalDate localDate = LocalDate.now();
        Date data = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        historicoWorkflow.setRecebimentoHistorico(data);
        System.out.println("Demanda: " + historicoWorkflow.getDemandaHistorico().getIdDemanda());
        HistoricoWorkflow historicoWorkflowSalvo = historicoWorkflowService.save(historicoWorkflow);
        try{
            Demanda demandaHistorico = demandaService.findById(historicoWorkflowSalvo.getDemandaHistorico().getIdDemanda()).get();
            demandaHistorico.setHistoricoWorkflowUltimaVersao(historicoWorkflowSalvo);
            demandaService.save(demandaHistorico);
        }catch (Exception e){
            return ResponseEntity.badRequest().body("Erro ao setar último histórico de workflow da demanda: " + e.getMessage());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(historicoWorkflowSalvo);
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping("/demanda/{id}")
    public ResponseEntity<Object> findByIdDemanda(@PathVariable("id") Demanda demandaHistorico) {
        try {
            List<HistoricoWorkflow> historicoWorkflows = historicoWorkflowService.findByDemandaHistorico(demandaHistorico);
            if (historicoWorkflows.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhum histórico de workflow de demanda com id: " + demandaHistorico + " encontrado!");
            }
            LocalDateTime localDateTime = LocalDateTime.now();
            Date verificaData = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
                historicoWorkflows.forEach(historicoWorkflow -> {
                if(historicoWorkflow.getConclusaoHistorico() != null){
                    if(historicoWorkflow.getConclusaoHistorico().before(verificaData)){
                        historicoWorkflow.setStatusWorkflow(StatusWorkflow.ATRASADO);
                    }
                }
            });
            return ResponseEntity.status(HttpStatus.FOUND).body(historicoWorkflows);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar histórico de workflow: " + e.getMessage());
        }
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping("/responsavel/{numeroCadastroResponsavel}")
    public ResponseEntity<Object> findByIdUsuario(@PathVariable("numeroCadastroResponsavel") Usuario numeroCadastroResponsavel) {
        try {
            List<HistoricoWorkflow> historicoWorkflows = historicoWorkflowService.findByIdResponsavel(numeroCadastroResponsavel);
            if (historicoWorkflows.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhum histórico de workflow encontrado com o responsável de número de cadastro " + numeroCadastroResponsavel + "!");
            }
            return ResponseEntity.status(HttpStatus.FOUND).body(historicoWorkflows);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar histórico de workflow: " + e.getMessage());
        }
    }

    //*****INCOMPLETO******
//    @CrossOrigin(origins = "http://localhost:3000")
//    @GetMapping("/recebimento-historico/{recebimentoHistorico}")
//    public ResponseEntity<Object> findByIdUsuario(@PathVariable("recebimentoHistorico") String recebimentoHistorico) {
//        try {
//            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
//            Date dataRecebimentoHistorico = formatter.parse(recebimentoHistorico);
//            List<HistoricoWorkflow> historicoWorkflows = historicoWorkflowService.findByRecebimentoHistorico(dataRecebimentoHistorico);
//            if (historicoWorkflows.isEmpty()) {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhum histórico de workflow encontrado com a data: " + dataRecebimentoHistorico);
//            }
//            return ResponseEntity.status(HttpStatus.FOUND).body(historicoWorkflows);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body("Erro ao buscar histórico de workflow: " + e.getMessage());
//        }
//    }

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping("/status-workflow/{statusWorkflow}")
    public ResponseEntity<Object> findByIdUsuario(@PathVariable("statusWorkflow") StatusWorkflow statusWorkflow) {
        try {
            List<HistoricoWorkflow> historicoWorkflows = historicoWorkflowService.findByStatusWorkflow(statusWorkflow);
            if (historicoWorkflows.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhum histórico de workflow encontrado com o status: " + statusWorkflow);
            }
            return ResponseEntity.status(HttpStatus.FOUND).body(historicoWorkflows);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar histórico de workflow: " + e.getMessage());
        }
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @PutMapping("/atualiza-versao/{id}")
    public ResponseEntity<Object> atualizaVersao(@PathVariable Integer idHistoricoWorkflow, @RequestBody CadastroHistoricoWorkflowDTO historicoWorkflowDTO, Demanda demanda) {
        try {
            Optional<HistoricoWorkflow> historicoWorkflowOptional = historicoWorkflowService.findById(idHistoricoWorkflow);
            if (historicoWorkflowOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhum histórico de workflow encontrado com o id: " + idHistoricoWorkflow);
            }
            HistoricoWorkflow historicoWorkflow = historicoWorkflowOptional.get();
            BeanUtils.copyProperties(historicoWorkflowDTO, historicoWorkflow);
            HistoricoWorkflow historicoWorkflowAnterior = demanda.getHistoricoWorkflowUltimaVersao();
            if (historicoWorkflow.getDemandaHistorico().equals(historicoWorkflowAnterior.getDemandaHistorico())) {
                historicoWorkflow.setVersaoHistorico(historicoWorkflowAnterior.getVersaoHistorico());
            } else {
                historicoWorkflow.setVersaoHistorico(historicoWorkflowAnterior.getVersaoHistorico() + 0.1);
            }
            return ResponseEntity.status(HttpStatus.OK).body(historicoWorkflowService.save(historicoWorkflow));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao atualizar histórico de workflow: " + e.getMessage());
        }
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @PutMapping("/atualiza-status/{id}")
    public ResponseEntity<Object> atualizaStatus(@PathVariable Integer idHistoricoWorkflow, @RequestBody CadastroHistoricoWorkflowDTO historicoWorkflowDTO, Demanda demanda) {
        try {
            Optional<HistoricoWorkflow> historicoWorkflowOptional = historicoWorkflowService.findById(idHistoricoWorkflow);
            if (historicoWorkflowOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhum histórico de workflow encontrado com o id: " + idHistoricoWorkflow);
            }
            HistoricoWorkflow historicoWorkflow = historicoWorkflowOptional.get();
            BeanUtils.copyProperties(historicoWorkflowDTO, historicoWorkflow);
            historicoWorkflow.setStatusWorkflow(StatusWorkflow.CONCLUIDO);
            return ResponseEntity.status(HttpStatus.OK).body(historicoWorkflowService.save(historicoWorkflow));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao atualizar histórico de workflow: " + e.getMessage());
        }
    }


    @CrossOrigin(origins = "http://localhost:3000")
    @DeleteMapping("/{idHistoricoWorkflow}")
    public ResponseEntity<Object> delete(@PathVariable Integer idHistoricoWorkflow) {
        try {
            Optional<HistoricoWorkflow> historicoWorkflowOptional = historicoWorkflowService.findById(idHistoricoWorkflow);
            if (historicoWorkflowOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhum histórico de workflow encontrado com o id: " + idHistoricoWorkflow);
            }
            historicoWorkflowService.deleteById(idHistoricoWorkflow);
            return ResponseEntity.status(HttpStatus.OK).body("Histórico de workflow com id: " + idHistoricoWorkflow + " deletado com sucesso!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao deletar histórico de workflow: " + e.getMessage());
        }
    }

}
