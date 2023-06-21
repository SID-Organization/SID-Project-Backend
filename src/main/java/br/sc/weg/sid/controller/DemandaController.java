package br.sc.weg.sid.controller;

import br.sc.weg.sid.DTO.*;
import br.sc.weg.sid.model.entities.*;
import br.sc.weg.sid.model.enums.*;
import br.sc.weg.sid.model.exporter.DemandaExcelExporter;
import br.sc.weg.sid.model.service.*;
import br.sc.weg.sid.utils.DemandaUtil;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@CrossOrigin
@RequestMapping("/sid/api/demanda")
@AllArgsConstructor
public class DemandaController {

    HistoricoWorkflowController historicoWorkflowController;

    HistoricoWorkflowService historicoWorkflowService;

    DemandaService demandaService;

    MotivoRecusaService motivoRecusaService;

    PdfDemandaService pdfDemandaService;

    UsuarioService usuarioService;

    BusinessUnityService businessUnityService;

    ArquivoDemandaService arquivoDemandaService;

    BeneficioService beneficioService;

    ForumService forumService;

    GerarPDFDemandaController gerarPDFDemandaController;

    SimpMessagingTemplate simpMessagingTemplate;

    NotificacaoService notificacaoService;

    ExcelExporterService excelExporterService;

    /**
     * Retorna uma lista de demandas resumidas.
     * <p>
     * Este endpoint retorna uma lista de demandas resumidas, filtrando apenas as demandas que não
     * estão no status "rascunho". Caso não haja demandas a serem retornadas, uma mensagem de erro é exibida falando: Nenhuma demanda encontrada.
     * As demandas são retornadas como uma lista de objetos do tipo DemandaResumida.
     *
     * @return ResponseEntity<List < DemandaResumida>> Lista de demandas resumidas
     */
    @GetMapping()
    public ResponseEntity<Object> findAll() {
        List<Demanda> demandas = demandaService.findAll();
        List<Demanda> demandasFiltradas = new ArrayList<>();
        if (demandas.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhuma demanda encontrada");
        }
        for (Demanda demanda : demandas) {
            if (demanda.getStatusDemanda() != StatusDemanda.RASCUNHO) {
                demandasFiltradas.add(demanda);
            }
        }
        DemandaUtil demandaUtil = new DemandaUtil();
        List<DemandaResumida> demandasResumidas = demandaUtil.resumirDemanda(demandasFiltradas);
        return ResponseEntity.status(HttpStatus.OK).body(demandasResumidas);
    }

    /**
     * Retorna uma lista de demandas contendo o ID e o título de todas as demandas.
     * <p>
     * Este endpoint retorna uma lista de objetos que contém o ID e o título de todas as demandas cadastradas no sistema, bem como o status
     * atual de cada demanda. Caso não haja demandas a serem retornadas, uma mensagem de erro é exibida.
     * As demandas são retornadas como uma lista de objetos do tipo Map<String, Object>.
     *
     * @return ResponseEntity<List < Map < String, Object>>> Lista de objetos contendo o ID e o título de todas as demandas.
     */
    @GetMapping("/titulos-id-demanda")
    public ResponseEntity<Object> findAllTitles() {
        List<Demanda> demandas = demandaService.findAll();
        if (demandas.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhuma demanda encontrada");
        }

        List<Map<String, Object>> demandasResumidas = demandas.stream().map(demanda -> {
            Map<String, Object> demandaResumida = new HashMap<>();
            demandaResumida.put("idDemanda", demanda.getIdDemanda());
            demandaResumida.put("tituloDemanda", demanda.getTituloDemanda());
            demandaResumida.put("statusDemanda", demanda.getStatusDemanda());
            return demandaResumida;
        }).collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.OK).body(demandasResumidas);
    }

    /**
     * Retorna uma lista resumida de todas as demandas que estão no status "Rascunho".
     * <p>
     * Este endpoint retorna uma lista resumida de todas as demandas que estão no status "Rascunho".
     * Caso não haja demandas a serem retornadas, uma mensagem de erro é exibida falando: Nenhuma demanda encontrada.
     * As demandas são retornadas como uma lista de objetos do tipo DemandaResumida.
     *
     * @return ResponseEntity<List < DemandaResumida>> Lista resumida de todas as demandas no status "Rascunho".
     */
    @GetMapping("/rascunhos")
    public ResponseEntity<Object> findAllRascunhos() {
        List<Demanda> demandas = demandaService.findByStatusDemanda(StatusDemanda.RASCUNHO);
        if (demandas.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhuma demanda encontrada");
        }
        DemandaUtil demandaUtil = new DemandaUtil();
        List<DemandaResumida> demandasResumidas = demandaUtil.resumirDemanda(demandas);
        return ResponseEntity.status(HttpStatus.OK).body(demandasResumidas);
    }


    /**
     * Cadastra uma nova demanda.
     * <p>
     * Esta função cadastroDemanda é responsável por cadastrar uma nova demanda no sistema.
     * Ela recebe três parâmetros: demandaJson, pdfDemandaJson e additionalFiles.
     * <p>
     * No início da função, são feitas as conversões necessárias das Strings em objetos DTO e em objetos modelo do sistema.
     * Também é feita a verificação dos atributos da demanda para determinar se ela está completa ou em rascunho.
     * Caso a demanda esteja completa, é feita a verificação de se o usuário que está cadastrando a demanda é um usuário
     * <p>
     * Após isso, a demanda é salva no banco de dados e, se houver arquivos adicionais, estes também são salvos e associados à demanda.
     * <p>
     * A função retorna um objeto do tipo ResponseEntity, que pode ter os seguintes valores:
     * <p>
     * 1- HttpStatus.CREATED (código 201): Indica que a demanda foi criada com sucesso e retorna a demanda salva.
     * 2- HttpStatus.BAD_REQUEST (código 400): Indica que houve algum erro na validação dos parâmetros ou na
     * tentativa de salvar os arquivos adicionais ou o PDF.
     * 3- HttpStatus.CONFLICT (código 409): Indica que houve um conflito na criação da demanda, qualquer tipo de erro na criação,
     * desde não conseguir cadastrar um benefício até não conseguir transformar um JSON em algum objeto.
     * <p>
     * Em caso de erro, a função retorna uma mensagem explicativa do erro ocorrido.
     *
     * @param demandaJson     JSON contendo as informações da demanda a ser cadastrada.
     * @param pdfDemandaJson  JSON contendo as informações do PDF da demanda a ser cadastrado.
     * @param additionalFiles Arquivos adicionais da demanda, se houver, como por exemplo uma imagem ou documento.
     * @return ResponseEntity contendo o objeto Demanda salvo no banco de dados.
     * Em caso de erro, retorna uma mensagem de erro com o respectivo código HTTP.
     */
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Object> cadastroDemanda(
            @RequestParam("demandaForm") @Valid String demandaJson,
            @RequestParam("pdfDemandaForm") @Valid String pdfDemandaJson,
            @RequestParam(value = "arquivosDemanda", required = false) MultipartFile[] additionalFiles
    ) {
        try {
            DemandaUtil demandaUtil = new DemandaUtil();
            CadastroDemandaDTO cadastroDemandaDTO = demandaUtil.convertToDto(demandaJson);
            CadastroPdfDemandaDTO cadastroPdfDemandaDTO = demandaUtil.convertToPdfDto(pdfDemandaJson);
            Demanda demanda = demandaUtil.convertDtoToModel(cadastroDemandaDTO);
            PdfDemanda pdfDemanda = demandaUtil.convertPdfDtoToModel(cadastroPdfDemandaDTO);
            demanda.setStatusDemanda(StatusDemanda.ABERTA);

            Date dataAtual = new Date();
            demanda.setDataCriacaoDemanda(dataAtual);

            //Verifica se a demanda possui todos os campos preenchidos, caso algum atributo esteja nulo, o status será RASCUNHO
            Class<? extends CadastroDemandaDTO> classe = cadastroDemandaDTO.getClass();
            List<Field> atributos = Arrays.asList(classe.getDeclaredFields());
            atributos.forEach(atributo -> {
                try {
                    Object valor = atributo.get(cadastroDemandaDTO);
                    if (!atributo.getName().equals("descricaoQualitativoDemanda")) {
                        if (valor == null || valor.equals("")) {
                            demanda.setStatusDemanda(StatusDemanda.RASCUNHO);
                        }
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            });
            Usuario usuarioDemanda = usuarioService.findById(cadastroDemandaDTO.getSolicitanteDemanda().getNumeroCadastroUsuario()).get();
            demanda.setBuSolicitanteDemanda(businessUnityService.findById(usuarioDemanda.getDepartamentoUsuario().getIdBusinessUnity()).get());

            demanda.setScoreDemanda(demandaUtil.retornaScoreDemandaCriacao(demanda));

            Demanda demandaSalva = demandaService.save(demanda);

            //essa variável tem como objetivo buscar a data do dia atual para ser inserida no arquivo de demanda
            LocalDate localDate = LocalDate.now();
            Date dataRegistroArquivo = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            ArquivoDemanda arquivoDemandaSalvo = new ArquivoDemanda();
            //Cadastra os arquivos da demanda
            if (additionalFiles != null) {
                try {
                    for (MultipartFile additionalImage : additionalFiles) {
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
            if (cadastroDemandaDTO.getBeneficiosDemanda() != null) {
                for (Beneficio beneficio : cadastroDemandaDTO.getBeneficiosDemanda()) {
                    beneficio.setDemandaBeneficio(demandaSalva);
                    beneficioService.save(beneficio);
                }
            }
            try {
                pdfDemanda.setDemanda(demandaSalva);
                pdfDemandaService.save(pdfDemanda);
            } catch (Exception e) {
                demandaService.deleteById(demandaSalva.getIdDemanda());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erro ao salvar pdf: " + e.getMessage());
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(demandaSalva);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Erro ao cadastrar demanda: " + e.getMessage());
        }
    }

    /**
     * Busca uma demanda pelo ID.
     *
     * @param id o ID da demanda a ser buscada
     * @return ResponseEntity contendo a demanda encontrada e o status HTTP OK,
     * ou uma mensagem de erro e o status HTTP NOT_FOUND caso a demanda não seja encontrada
     */
    @GetMapping("/id/{id}")
    public ResponseEntity<Object> findById(@PathVariable("id") Integer id) {
        try {
            Demanda demanda = demandaService.findById(id).get();
            return ResponseEntity.status(HttpStatus.OK).body(demanda);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Demanda com id: " + id + " não encontrada!");
        }
    }

    /**
     * Retorna uma lista de demandas resumidas de acordo com o status de demanda fornecido.
     *
     * @param statusDemanda O status de demanda pelo qual as demandas serão filtradas.
     * @return ResponseEntity com uma lista de demandas resumidas se houverem demandas com o status fornecido,
     * caso contrário, retorna uma mensagem de erro indicando que não existem demandas com o status fornecido.
     * <p>
     * Se ocorrer algum erro ao buscar as demandas, retorna uma mensagem de erro indicando o problema.
     */
    @GetMapping("/statusDemanda/{statusDemanda}")
    public ResponseEntity<Object> findByStatus(@PathVariable("statusDemanda") StatusDemanda statusDemanda) {
        try {
            List<Demanda> demandas = demandaService.findByStatusDemanda(statusDemanda);
            if (demandas.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Não existem demandas com status: " + statusDemanda);
            }
            DemandaUtil demandaUtil = new DemandaUtil();
            List<DemandaResumida> demandasResumidas = demandaUtil.resumirDemanda(demandas);
            return ResponseEntity.status(HttpStatus.OK).body(demandasResumidas);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Erro ao buscar demandas por status: " + e.getMessage());
        }
    }

    /**
     * Retorna uma lista de demandas resumidas associadas ao número de cadastro do solicitante fornecido.
     *
     * @param numeroCadastroSoliciante O número de cadastro do solicitante pelo qual as demandas serão filtradas.
     * @return ResponseEntity com uma lista de demandas resumidas se houverem demandas associadas ao solicitante,
     * caso contrário, retorna uma mensagem de erro indicando que o solicitante não possui demandas.
     */
    @GetMapping("/solicitante/{numeroCadastroSoliciante}")
    public ResponseEntity<Object> findBySolicitante(@PathVariable("numeroCadastroSoliciante") Integer numeroCadastroSoliciante) {
        try {
            Usuario solicitanteDemanda = usuarioService.findById(numeroCadastroSoliciante).get();
            List<Demanda> demandas = demandaService.findBySolicitanteDemanda(solicitanteDemanda);
            if (demandas.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("O solicitante " + solicitanteDemanda.getNomeUsuario() + " não possui demandas!");
            }
            DemandaUtil demandaUtil = new DemandaUtil();
            List<DemandaResumida> demandasResumidas = demandaUtil.resumirDemanda(demandas);
            return ResponseEntity.status(HttpStatus.OK).body(demandasResumidas);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Solicitante com matrícula: " + numeroCadastroSoliciante + " não encontrado!" + e.getMessage());
        }
    }

    /**
     * Retorna uma lista de demandas resumidas em ordem decrescente de data de prazo de elaboração (mais nova a mais antiga).
     *
     * @return ResponseEntity com a lista de demandas resumidas, ordenadas por data de prazo de elaboração decrescente,
     * caso hajam demandas cadastradas, ou uma mensagem de erro indicando que não foram encontradas demandas.
     */
    @GetMapping("/data-decrescente")
    public ResponseEntity<Object> findByDataDecrescente() {
        DemandaUtil demandaUtil = new DemandaUtil();
        List<Demanda> demandas = demandaService.findByPrazoElaboracaoDemandaDesc();
        if (demandas.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhuma demanda encontrada!");
        }
        List<DemandaResumida> demandasResumidas = demandaUtil.resumirDemanda(demandas);
        return ResponseEntity.status(HttpStatus.OK).body(demandasResumidas);
    }

    //Busca demanda por data de criação (mais antiga a mais nova)

    /**
     * Retorna uma lista de demandas resumidas em ordem crescente de data de prazo de elaboração (mais antiga a mais nova).
     *
     * @return ResponseEntity com a lista de demandas resumidas, ordenadas por data de prazo de elaboração crescente,
     * caso hajam demandas cadastradas, ou uma mensagem de erro indicando que não foram encontradas demandas.
     */
    @GetMapping("/data-crescente")
    public ResponseEntity<Object> findByDataCrescente() {
        DemandaUtil demandaUtil = new DemandaUtil();
        List<Demanda> demandas = demandaService.findByPrazoElaboracaoDemandaAsc();
        if (demandas.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhuma demanda encontrada!");
        }
        List<DemandaResumida> demandasResumidas = demandaUtil.resumirDemanda(demandas);
        return ResponseEntity.status(HttpStatus.OK).body(demandasResumidas);
    }


    /**
     * Retorna uma lista de demandas resumidas de acordo com o score de demanda fornecido.
     *
     * @param score O score de demanda pelo qual as demandas serão filtradas.
     * @return ResponseEntity com uma lista de demandas resumidas se houverem demandas com o score fornecido,
     * caso contrário, retorna uma mensagem de erro indicando que não existem demandas com o score fornecido.
     */
    @GetMapping("/score/{score}")
    public ResponseEntity<Object> findByScore(@PathVariable("score") Double score) {
        DemandaUtil demandaUtil = new DemandaUtil();
        List<Demanda> demandas = demandaService.findByScoreDemanda(score);
        if (demandas.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhuma demanda com score: " + score + " foi encontrada!");
        }
        List<DemandaResumida> demandasResumidas = demandaUtil.resumirDemanda(demandas);
        return ResponseEntity.status(HttpStatus.OK).body(demandasResumidas);
    }


    /**
     * Retorna uma lista de demandas resumidas de acordo com o título da demanda fornecido.
     *
     * @param tituloDemanda O título da demanda pelo qual as demandas serão filtradas.
     * @return ResponseEntity com uma lista de demandas resumidas se houverem demandas com o título fornecido,
     * caso contrário, retorna uma mensagem de erro indicando que não existem demandas com o título fornecido.
     */
    @GetMapping("/titulo-demanda/{tituloDemanda}")
    public ResponseEntity<Object> findByTituloDemanda(@PathVariable("tituloDemanda") String tituloDemanda) {
        DemandaUtil demandaUtil = new DemandaUtil();
        List<Demanda> demandas = demandaService.findByTituloDemanda(tituloDemanda);
        if (demandas.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhuma demanda com título: " + tituloDemanda + " foi encontrada!");
        }
        List<DemandaResumida> demandasResumidas = demandaUtil.resumirDemanda(demandas);
        return ResponseEntity.status(HttpStatus.OK).body(demandasResumidas);
    }

    /**
     * Retorna uma lista de demandas resumidas de acordo com o tamanho de demanda fornecido.
     *
     * @param tamanhoDemanda O tamanho de demanda pelo qual as demandas serão filtradas.
     * @return ResponseEntity com uma lista de demandas resumidas se houverem demandas com o tamanho fornecido,
     * caso contrário, retorna uma mensagem de erro indicando que não existem demandas com o tamanho fornecido.
     */
    @GetMapping("/tamanhoDemanda/{tamanhoDemanda}")
    public ResponseEntity<Object> findByTamanho(@PathVariable("tamanhoDemanda") TamanhoDemanda tamanhoDemanda) {
        DemandaUtil demandaUtil = new DemandaUtil();
        List<Demanda> demandas = demandaService.findByTamanhoDemanda(tamanhoDemanda);
        if (demandas.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhuma demanda com tamanho: " + tamanhoDemanda + " foi encontrada!");
        }
        List<DemandaResumida> demandasResumidas = demandaUtil.resumirDemanda(demandas);
        return ResponseEntity.status(HttpStatus.OK).body(demandasResumidas);
    }

    /**
     * Retorna as demandas associadas a um analista, filtrando as que são de sua responsabilidade e que não estão em status de rascunho.
     * <p>
     * Filtra as demandas que são de sua responsabilidade e que não estão em status de rascunho.
     *
     * @param numeroCadastroAnalista o número de cadastro do analista.
     * @return um objeto ResponseEntity contendo a lista de demandas associadas ao analista, com
     * informações resumidas, ou uma mensagem de erro caso o analista não seja encontrado ou não possua demandas.
     */
    @GetMapping("/analista/{numeroCadastroAnalista}")
    public ResponseEntity<Object> findByAnalista(@PathVariable("numeroCadastroAnalista") Integer numeroCadastroAnalista) {
        try {
            Usuario analistaDemanda = usuarioService.findById(numeroCadastroAnalista).get();
            if (analistaDemanda.getCargoUsuario() != Cargo.ANALISTA) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("O usuário " + analistaDemanda.getNomeUsuario() + " não é um analista! " +
                        "Ele é um " + analistaDemanda.getCargoUsuario().getNome());
            }
            List<Demanda> demandas = demandaService.findByAnalistaResponsavelDemanda(analistaDemanda);
            List<Demanda> demandasFiltradas = new ArrayList<>();
            if (demandas.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("O analista " + analistaDemanda.getNomeUsuario() + " não possui demandas!");
            }
            for (Demanda demanda : demandas) {
                if (demanda.getSolicitanteDemanda() != analistaDemanda && demanda.getAnalistaResponsavelDemanda() == analistaDemanda &&
                        demanda.getStatusDemanda() != StatusDemanda.RASCUNHO) {
                    demandasFiltradas.add(demanda);
                }
            }
            DemandaUtil demandaUtil = new DemandaUtil();
            List<DemandaResumida> demandasResumidas = demandaUtil.resumirDemanda(demandasFiltradas);
            return ResponseEntity.status(HttpStatus.OK).body(demandasResumidas);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Retorna as demandas associadas ao gerente da área com o número de cadastro fornecido.
     * <p>
     * Filtra as demandas que são de sua responsabilidade e que não estão em status de rascunho.
     *
     * @param numeroCadastroGerente o número de cadastro do gerente da área
     * @return ResponseEntity com a lista de demandas associadas ao gerente da área, com informações resumidas,
     * ou uma mensagem de erro caso o gerente não seja encontrado ou não possua demandas
     */
    @GetMapping("/gerente-da-area/{numeroCadastroUsuario}")
    public ResponseEntity<Object> findByGerente(@PathVariable("numeroCadastroUsuario") Integer numeroCadastroGerente) {
        try {
            Usuario gerenteDaAreaDemanda = usuarioService.findById(numeroCadastroGerente).get();
            if (gerenteDaAreaDemanda.getCargoUsuario() != Cargo.GERENTE) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("O usuário " + gerenteDaAreaDemanda.getNomeUsuario() + " não é um gerente! " +
                        "Ele é um " + gerenteDaAreaDemanda.getCargoUsuario().getNome());
            }
            List<Demanda> demandas = demandaService.findByGerenteDaAreaDemanda(gerenteDaAreaDemanda);
            if (demandas.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("O gerente " + gerenteDaAreaDemanda.getNomeUsuario() + " não possui demandas!");
            }
            DemandaUtil demandaUtil = new DemandaUtil();
            List<DemandaResumida> demandasResumidas = demandaUtil.resumirDemanda(demandas);
            return ResponseEntity.status(HttpStatus.OK).body(demandasResumidas);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhum usuário com o número de cadastro: " + numeroCadastroGerente + " foi encontrado!");
        }
    }

    /**
     * Endpoint que busca todas as demandas associadas a um gestor de TI específico.
     * <p>
     * Filtra as demandas que são de sua responsabilidade e que não estão em status de rascunho.
     *
     * @param numeroCadastroGestor o número de cadastro do gestor de TI
     * @return uma resposta HTTP contendo as demandas encontradas em formato resumido ou uma mensagem de erro caso não sejam
     * encontradas ou o usuário não seja um gestor de TI
     */
    @GetMapping("/gestor-ti/{numeroCadastroUsuario}")
    public ResponseEntity<Object> findByGestor(@PathVariable("numeroCadastroUsuario") Integer numeroCadastroGestor) {
        try {
            Usuario gestorDeTiDemanda = usuarioService.findById(numeroCadastroGestor).get();
            if (gestorDeTiDemanda.getCargoUsuario() != Cargo.GESTOR_TI) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("O usuário " + gestorDeTiDemanda.getNomeUsuario() + " não é um gestor! " +
                        "Ele é um " + gestorDeTiDemanda.getCargoUsuario().getNome());
            }
            List<Demanda> demandas = demandaService.findByGestorResponsavelDemanda(gestorDeTiDemanda);
            if (demandas.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("O gestor " + gestorDeTiDemanda.getNomeUsuario() + " não possui demandas!");
            }
            DemandaUtil demandaUtil = new DemandaUtil();
            List<DemandaResumida> demandasResumidas = demandaUtil.resumirDemanda(demandas);
            return ResponseEntity.status(HttpStatus.OK).body(demandasResumidas);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }


    /**
     * Atualiza o status de uma demanda com o ID fornecido e retorna a demanda atualizada.
     * <p>
     * Se a demanda não for encontrada, retorna uma mensagem de erro.
     * Se o status da demanda for CANCELADA, o analista responsável pela demanda, o gerente da area e o gestor de t.i serão removidos da demanda.
     *
     * @param idDemanda   o ID da demanda a ser atualizada
     * @param requestBody um objeto Map que contém o novo status da demanda
     * @return um ResponseEntity contendo a demanda atualizada ou uma mensagem de erro se a demanda não foi encontrada
     */
    @PutMapping("/status/{id}")
    public ResponseEntity<Object> atualizarStatusDemanda(
            @PathVariable("id") Integer idDemanda,
            @RequestBody Map<String, String> requestBody) {

        StatusDemanda statusDemanda = StatusDemanda.valueOf(requestBody.get("statusDemanda"));
        boolean edicao;

        if (!demandaService.existsById(idDemanda)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Não foi encontrado a demanda com o id " + idDemanda);
        }
        Demanda demanda = demandaService.findById(idDemanda).get();
        if (demanda.getStatusDemanda() == StatusDemanda.RASCUNHO) {
            edicao = false;
        } else {
            edicao = true;
        }
        demanda.setStatusDemanda(statusDemanda);
        Demanda demandaAtualizada = demandaService.save(demanda);
        //Esse LocalDateTime serve para adicionarmos nas notificações criadas nessa atualização de status
        LocalDateTime horarioDataNow = LocalDateTime.now();
        DateTimeFormatter formatar = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String notificacaoHoraData = horarioDataNow.format(formatar) + " - " + horarioDataNow.format(dateFormatter);

        if (demandaAtualizada.getStatusDemanda() == StatusDemanda.ABERTA) {
            Notificacao notificacaoDemandaCriada = new Notificacao();
            notificacaoDemandaCriada.setTextoNotificacao("uma demanda foi criada! " + demandaAtualizada.getTituloDemanda() + " criada por: " +
                    demandaAtualizada.getSolicitanteDemanda().getNomeUsuario());
            notificacaoDemandaCriada.setTipoNotificacao("approved");
            notificacaoDemandaCriada.setResponsavel(demandaAtualizada.getSolicitanteDemanda().getNomeUsuario());
            notificacaoDemandaCriada.setLinkNotificacao("/demandas/" + demandaAtualizada.getIdDemanda());
            notificacaoDemandaCriada.setUsuario(demandaAtualizada.getAnalistaResponsavelDemanda());
            notificacaoDemandaCriada.setTempoNotificacao(notificacaoHoraData);
            notificacaoDemandaCriada.setVisualizada(false);
            simpMessagingTemplate.convertAndSend("/notificacao-demanda-cadastro/analista/" +
                    demandaAtualizada.getAnalistaResponsavelDemanda().getNumeroCadastroUsuario(), notificacaoDemandaCriada);
            notificacaoService.save(notificacaoDemandaCriada);
        } else {
            Notificacao notificacaoStatus = new Notificacao();
            notificacaoStatus.setTextoNotificacao("a demanda " + demandaAtualizada.getIdDemanda() + " - "
                    + demandaAtualizada.getTituloDemanda() + " teve seu status alterado para " + demandaAtualizada.getStatusDemanda().getNome().toLowerCase());
            atualizaTipoNotificacao(demandaAtualizada, notificacaoStatus);
            notificacaoStatus.setUsuario(demandaAtualizada.getSolicitanteDemanda());
            notificacaoStatus.setTempoNotificacao(notificacaoHoraData);
            notificacaoStatus.setResponsavel(demandaAtualizada.getAnalistaResponsavelDemanda().getNomeUsuario());
            notificacaoStatus.setLinkNotificacao("/demandas/" + demandaAtualizada.getIdDemanda());
            notificacaoStatus.setVisualizada(false);
            simpMessagingTemplate.convertAndSend("/notificacao-usuario-status/" +
                    demandaAtualizada.getSolicitanteDemanda().getNumeroCadastroUsuario(), notificacaoStatus);
            notificacaoService.save(notificacaoStatus);
        }

        //Se a demanda tiver em status Aberta(Backlog) um histórico de workflow é criado
        if (statusDemanda.equals(StatusDemanda.ABERTA)) {
            CadastroHistoricoWorkflowDTO historicoWorkflowDTO = new CadastroHistoricoWorkflowDTO();
            try {
                historicoWorkflowDTO.setDemandaHistorico(demandaAtualizada);
                if (edicao == false) {
                    historicoWorkflowDTO.setTarefaHistoricoWorkflow(TarefaWorkflow.PREENCHER_DEMANDA);
                    historicoWorkflowDTO.setIdResponsavel(demandaAtualizada.getSolicitanteDemanda());
                    historicoWorkflowController.cadastroHistoricoWorkflow(historicoWorkflowDTO);
                } else {
                    historicoWorkflowDTO.setAcaoFeitaHistoricoAnterior("Enviar");
                }
                historicoWorkflowDTO.setTarefaHistoricoWorkflow(TarefaWorkflow.CLASSIFICACAO_APROVACAO);
                historicoWorkflowDTO.setIdResponsavel(demandaAtualizada.getAnalistaResponsavelDemanda());
                try {
                    historicoWorkflowController.cadastroHistoricoWorkflow(historicoWorkflowDTO);
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erro ao salvar histórico de workflow: " + e.getMessage());
            }
        }
        return ResponseEntity.status(HttpStatus.OK).body(demanda);
    }

    private void atualizaTipoNotificacao(Demanda demandaAtualizada, Notificacao notificacaoStatus) {
        if (demandaAtualizada.getStatusDemanda() == StatusDemanda.APROVADO_PELO_GERENTE_DA_AREA ||
                demandaAtualizada.getStatusDemanda() == StatusDemanda.APROVADA_EM_COMISSAO) {
            notificacaoStatus.setTipoNotificacao("approved");
        } else if (demandaAtualizada.getStatusDemanda() == StatusDemanda.CANCELADA) {
            notificacaoStatus.setTipoNotificacao("rejected");
        } else {
            notificacaoStatus.setTipoNotificacao("edited");
        }
    }

    /**
     * Método responsável por atualizar uma demanda existente através do seu id.
     * <p>
     * Caso a demanda não seja encontrada, retorna uma mensagem de erro.
     * Caso a demanda seja encontrada, atualiza os dados da demanda e retorna a demanda atualizada.
     *
     * @param idDemanda              Id da demanda a ser atualizada.
     * @param demandaJson            String em formato JSON contendo os dados da demanda a ser atualizada.
     * @param pdfDemandaJson         String em formato JSON contendo os dados do PDF associado à demanda a ser atualizada.
     * @param additionalFiles        Array de arquivos adicionais relacionados à demanda a ser atualizada.
     * @param atualizaVersaoWorkflow Um String para sabermos se a versão do histórico será atualizado ou não.
     * @return ResponseEntity contendo a demanda atualizada ou uma mensagem de erro, caso não seja possível atualizar a demanda.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Object> atualizarDemanda(
            @PathVariable("id") Integer idDemanda,
            @RequestParam("demandaForm") @Valid String demandaJson,
            @RequestParam("pdfDemandaForm") @Valid String pdfDemandaJson,
            @RequestParam(value = "arquivosDemanda", required = false) MultipartFile[] additionalFiles,
            @RequestParam(value = "atualizaVersaoWorkflow", required = false) @Valid String atualizaVersaoWorkflow
    ) {
        DemandaUtil demandaUtil = new DemandaUtil();
        Demanda demandaExiste = demandaService.findById(idDemanda).get();
        CadastroDemandaDTO cadastroDemandaDTO = demandaUtil.convertToDto(demandaJson);
        CadastroPdfDemandaDTO cadastroPdfDemandaDTO = demandaUtil.convertToPdfDto(pdfDemandaJson);
        if (!demandaService.existsById(idDemanda)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Não foi encontrado a demanda com o id " + idDemanda);
        }
        Demanda demanda = demandaExiste;
        BeanUtils.copyProperties(cadastroDemandaDTO, demanda);
        if (demanda.getTamanhoDemanda() != null) {
            double score = demandaUtil.retornaScoreDemandaClassificacao(demanda);
            demanda.setScoreDemanda(score);
        } else {
            demanda.setScoreDemanda(demandaUtil.retornaScoreDemandaCriacao(demanda));
        }
        Demanda demandaAtualizada = demandaService.save(demanda);
        if (demanda.getBeneficiosDemanda() != null) {
            demanda.getBeneficiosDemanda().forEach(beneficio -> beneficio.setDemandaBeneficio(demandaAtualizada));
        }
        if (atualizaVersaoWorkflow != null) {
            historicoWorkflowController.atualizaVersaoWorkflow(demanda.getHistoricoWorkflowUltimaVersao().getIdHistoricoWorkflow(),
                    demanda.getHistoricoWorkflowUltimaVersao());
        }
        //essa variável tem como objetivo buscar a data do dia atual para ser inserida no arquivo de demanda
        LocalDate localDate = LocalDate.now();
        Date dataRegistroArquivo = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        ArquivoDemanda arquivoDemandaSalvo = new ArquivoDemanda();
        //Cadastra os arquivos da demanda
        if (additionalFiles != null) {
            try {
                for (MultipartFile additionalImage : additionalFiles) {
                    ArquivoDemanda arquivoDemanda = new ArquivoDemanda();
                    arquivoDemanda.setNomeArquivo(additionalImage.getOriginalFilename());
                    arquivoDemanda.setTipoArquivo(additionalImage.getContentType());
                    arquivoDemanda.setArquivo(additionalImage.getBytes());
                    arquivoDemanda.setIdDemanda(demandaAtualizada);
                    arquivoDemanda.setIdUsuario(usuarioService.findById(cadastroDemandaDTO.getSolicitanteDemanda().getNumeroCadastroUsuario()).get());
                    arquivoDemanda.setDataRegistroArquivo(dataRegistroArquivo);
                    arquivoDemandaSalvo = arquivoDemandaService.save(arquivoDemanda);
                    demandaAtualizada.getArquivosDemandas().add(arquivoDemandaSalvo);
                }
            } catch (Exception e) {
                arquivoDemandaService.deleteById(arquivoDemandaSalvo.getIdArquivoDemanda());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erro ao salvar arquivos: " + e.getMessage());
            }
        }
        try {
            PdfDemanda pdfDemanda = demandaUtil.convertPdfDtoToModel(cadastroPdfDemandaDTO);
            pdfDemanda.setDemanda(demandaAtualizada);
            pdfDemandaService.save(pdfDemanda);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Não foi possível cadastrar o pdf da demanda!" + e.getMessage());
        }

        try {

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Não foi possível gerar o pdf da demanda!" + e.getMessage());
        }
        return ResponseEntity.status(HttpStatus.OK).body(demandaAtualizada);
    }

    /**
     * Atualiza as informações de uma demanda adicionando as bus beneficiados, bu solicitante, tamanho da demanda e a secao de ti responsável.
     *
     * @param id                                o ID da demanda a ser atualizada
     * @param cadastroBusBeneficiadasDemandaDTO o DTO contendo as informações atualizadas da demanda
     * @param additionalFiles                   os arquivos adicionais a serem salvos junto com a demanda (opcional)
     * @return um ResponseEntity contendo a demanda atualizada ou uma mensagem de erro, caso ocorra
     * @throws Exception se ocorrer algum erro durante o processo de atualização da demanda ou salvamento dos arquivos
     */
    @PutMapping("/atualiza-bus-beneficiadas/{id}")
    public ResponseEntity<Object> atualizaBusBeneficiadas(
            @PathVariable("id") Integer id,
            @RequestBody @Valid CadastroBusBeneficiadasDemandaDTO cadastroBusBeneficiadasDemandaDTO,
            @RequestBody @Valid MultipartFile[] additionalFiles
    ) throws Exception {

        if (!demandaService.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Não foi encontrado a demanda com o id: " + id);
        }
        Demanda demanda = demandaService.findById(id).get();
        BeanUtils.copyProperties(cadastroBusBeneficiadasDemandaDTO, demanda);
        demanda.setStatusDemanda(StatusDemanda.CLASSIFICADO_PELO_ANALISTA);
        DemandaUtil demandaUtil = new DemandaUtil();
        Double valorScore = demandaUtil.retornaScoreDemandaClassificacao(demanda);
        demanda.setScoreDemanda(valorScore);
        Demanda demandaSalva = demandaService.save(demanda);
        gerarPDFDemandaController.generatePDF(demandaSalva.getIdDemanda());
        LocalDate localDate = LocalDate.now();
        Date dataRegistroArquivo = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        ArquivoDemanda arquivoDemandaSalvo = new ArquivoDemanda();
        if (additionalFiles != null) {
            try {
                for (MultipartFile additionalImage : additionalFiles) {
                    ArquivoDemanda arquivoDemanda = new ArquivoDemanda();
                    arquivoDemanda.setNomeArquivo(additionalImage.getOriginalFilename());
                    arquivoDemanda.setTipoArquivo(additionalImage.getContentType());
                    arquivoDemanda.setArquivo(additionalImage.getBytes());
                    arquivoDemanda.setIdDemanda(demandaSalva);
                    arquivoDemanda.setIdUsuario(usuarioService.findById(demandaSalva.getSolicitanteDemanda().getNumeroCadastroUsuario()).get());
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
        historicoWorkflowController.atualizaVersaoWorkflow(demanda.getHistoricoWorkflowUltimaVersao().getIdHistoricoWorkflow(),
                demanda.getHistoricoWorkflowUltimaVersao());
        gerarPDFDemandaController.generatePDF(demandaSalva.getIdDemanda());
        return ResponseEntity.status(HttpStatus.OK).body(demandaSalva);
    }


    /**
     * Busca as demandas em status de rascunho de um determinado solicitante.
     *
     * @param numeroCadastroUsuario o número de cadastro do usuário solicitante das demandas em rascunho.
     * @return um ResponseEntity contendo uma lista de objetos Demanda no corpo da resposta, caso haja demandas em rascunho do solicitante.
     * Retorna um ResponseEntity com status 404 Not Found e uma mensagem de erro, caso não haja nenhuma demanda em rascunho.
     * Retorna um ResponseEntity com status 400 Bad Request e uma mensagem de erro, caso ocorra algum erro ao buscar as demandas em rascunho.
     */
    @GetMapping("/rascunho/{numeroCadastroUsuario}")
    public ResponseEntity<Object> buscarDemandasRascunho(@PathVariable("numeroCadastroUsuario") Integer numeroCadastroUsuario) {
        try {
            DemandaUtil demandaUtil = new DemandaUtil();
            List<Demanda> demandas = demandaService.findRascunhosBySolicitanteDemanda(numeroCadastroUsuario);
            if (demandas.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Não foi encontrado nenhuma demanda com o status rascunho!");
            }
            List<DemandaResumida> demandasResumidas = demandaUtil.resumirDemanda(demandas);
            return ResponseEntity.status(HttpStatus.OK).body(demandasResumidas);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erro ao buscar demandas com status rascunho: " + e.getMessage());
        }
    }

    /**
     * Retorna o PDF da demanda com o ID informado.
     *
     * @param idDemanda ID da demanda a ser pesquisada.
     * @return ResponseEntity contendo o PDF da demanda, se encontrada. Caso contrário, retorna uma mensagem de erro.
     */
    @GetMapping("/pdf-demanda/{idDemanda}")
    ResponseEntity<Object> listarPropostaPdf(@PathVariable("idDemanda") Integer idDemanda) {
        try {
            if (demandaService.existsById(idDemanda)) {
                Demanda demanda = demandaService.findById(idDemanda).get();
                byte[] pdfBytes = demanda.getPdfDemanda();

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDisposition(ContentDisposition.builder("inline").filename("demanda-" + demanda.getTituloDemanda() + ".pdf").build());

                return ResponseEntity.ok().headers(headers).body(pdfBytes);
            } else {
                return ResponseEntity.badRequest().body("ERROR 0007: A proposta inserida não existe! ID PROPOSTA: " + idDemanda);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.badRequest().body("ERROR 0005: Erro ao buscar pdf da proposta de id: " + idDemanda + "!");
    }

    @PostMapping("/tabela-excel")
    public void gerarTabelaExcel(HttpServletResponse response, @RequestParam("demandaIdList") List<Integer> demandasIdList) throws IOException {
        excelExporterService.criarTabelaDemandaExcel(response, demandasIdList);
    }



    /**
     * Método responsável por deletar uma demanda do banco de dados.
     *
     * @param idDemanda Identificador da demanda a ser deletada.
     * @return ResponseEntity com status e mensagem de sucesso ou erro.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deletarDemanda(@PathVariable("id") Integer idDemanda) {
        try {
            if (!demandaService.existsById(idDemanda)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Não foi encontrado a demanda com o id " + idDemanda);
            }
            if (demandaService.findById(idDemanda).get().getStatusDemanda().equals(StatusDemanda.RASCUNHO)) {

                List<PdfDemanda> pdfDemanda = pdfDemandaService.findByDemanda(demandaService.findById(idDemanda).get());
                if (!pdfDemanda.isEmpty()) {
                    pdfDemandaService.deleteAll(pdfDemanda);
                }
                return ResponseEntity.status(HttpStatus.OK).body("Demanda com o id: " + idDemanda + " deletada com sucesso!");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Demanda com o id: " + idDemanda + " não pode ser deletada pois não tem o status rascunho!");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erro ao deletar a demanda: " + e.getMessage());
        }
    }

    /**
     * Deleta uma lista de demandas que estejam no status rascunho.
     *
     * @param demandas DTO contendo uma lista de ids das demandas a serem deletadas.
     * @return ResponseEntity com mensagem de sucesso em caso de sucesso ou exceção em caso de erro.
     */
    @PostMapping("/delete-lista-demanda")
    public ResponseEntity<Object> deletarDemandas(@RequestBody @Valid DeletaListaDemandaDTO demandas) {
        demandas.getDemandas().forEach(demanda -> {
            try {
                if (!demandaService.existsById(demanda.getIdDemanda())) {
                    throw new Exception("Não foi encontrado a demanda com o id " + demanda.getIdDemanda());
                }
                if (demandaService.findById(demanda.getIdDemanda()).get().getStatusDemanda().equals(StatusDemanda.RASCUNHO)) {
                    List<PdfDemanda> pdfDemanda = pdfDemandaService.findByDemanda(demandaService.findById(demanda.getIdDemanda()).get());
                    if (!pdfDemanda.isEmpty()) {
                        pdfDemandaService.deleteAll(pdfDemanda);
                    }
                } else {
                    throw new Exception("Demanda com o id: " + demanda.getIdDemanda() + " não pode ser deletada pois não tem o status rascunho!");
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e.getMessage());
            }
        });
        return ResponseEntity.status(HttpStatus.OK).body("Demandas deletadas com sucesso!");
    }

    /**
     * Deleta todas as demandas com status "rascunho" pertencentes a um determinado usuário.
     *
     * @param idUsuario o ID do usuário a quem as demandas pertencem.
     * @return um objeto ResponseEntity com o status HTTP e uma mensagem indicando o resultado da operação.
     */
    @DeleteMapping("/deleta-rascunhos/{idUsuario}")
    public ResponseEntity<Object> deletarRascunhos(@PathVariable("idUsuario") Integer idUsuario) {
        try {
            Usuario usuario = usuarioService.findById(idUsuario).get();
            List<Demanda> demandas = demandaService.findByStatusDemandaAndSolicitanteDemanda(StatusDemanda.RASCUNHO, usuario);
            if (demandas.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Não foi encontrado nenhuma demanda com o status rascunho!");
            }
            demandas.forEach(demanda -> {
                try {
                    List<PdfDemanda> pdfDemanda = pdfDemandaService.findByDemanda(demanda);
                    if (!pdfDemanda.isEmpty()) {
                        pdfDemandaService.deleteAll(pdfDemanda);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage());
                }
            });
            return ResponseEntity.status(HttpStatus.OK).body("Demandas deletadas com sucesso!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erro ao deletar demandas com status rascunho: " + e.getMessage());
        }
    }

    @PutMapping("/devolucao-demanda/{idDemanda}")
    public ResponseEntity<Object> devolverDemanda(@RequestBody DevolverDemandaDTO devolverDemandaDTO, @PathVariable("idDemanda") Integer idDemanda) throws Exception {
        Demanda demanda = demandaService.findById(idDemanda).get();
        BeanUtils.copyProperties(devolverDemandaDTO, demanda);
        demanda.setStatusDemanda(devolverDemandaDTO.getStatusDemanda());
        MotivoRecusa motivoRecusa = new MotivoRecusa();
        motivoRecusa.setDemandaMotivoRecusa(demanda);
        motivoRecusa.setDescricaoMotivoRecusa(devolverDemandaDTO.getMotivoRecusaDemanda());
        motivoRecusa.setStatusDemandaMotivoRecusa(devolverDemandaDTO.getStatusDemanda());

        if (demanda.getStatusDemanda() == StatusDemanda.CANCELADA) {
            HistoricoWorkflow historicoWorkflow = demanda.getHistoricoWorkflowUltimaVersao();
            LocalDateTime localDateTime = LocalDateTime.now();
            Date dataAtual = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
            historicoWorkflow.setConclusaoHistorico(dataAtual);
            historicoWorkflow.setStatusWorkflow(StatusWorkflow.CONCLUIDO);
            historicoWorkflow.setAcaoFeitaHistorico("Recusar");
            historicoWorkflow.setIdResponsavel(devolverDemandaDTO.getIdResponsavel());
            historicoWorkflow.setVersaoHistorico(historicoWorkflow.getVersaoHistorico() + 0.1);
            HistoricoWorkflow historicoWorkflowSalvo = historicoWorkflowService.save(historicoWorkflow);
            motivoRecusa.setIdHistoricoWorkflow(historicoWorkflowSalvo.getIdHistoricoWorkflow());
            demanda.setHistoricoWorkflowUltimaVersao(historicoWorkflowSalvo);
        } else if (demanda.getStatusDemanda() == StatusDemanda.EM_EDICAO) {
            HistoricoWorkflow historicoWorkflowAnterior = demanda.getHistoricoWorkflowUltimaVersao();
            historicoWorkflowAnterior.setStatusWorkflow(StatusWorkflow.CONCLUIDO);
            historicoWorkflowAnterior.setAcaoFeitaHistorico("Devolver");
            historicoWorkflowAnterior.setMotivoDevolucaoHistorico(devolverDemandaDTO.getMotivoRecusaDemanda());
            historicoWorkflowService.save(historicoWorkflowAnterior);
            HistoricoWorkflow historicoWorkflow = new HistoricoWorkflow();
            historicoWorkflow.setDemandaHistorico(demanda);
            historicoWorkflow.setIdResponsavel(demanda.getSolicitanteDemanda());
            LocalDateTime localDateTime = LocalDateTime.now();
            Date dataAtual = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
            historicoWorkflow.setRecebimentoHistorico(dataAtual);
            historicoWorkflow.setStatusWorkflow(StatusWorkflow.EM_ANDAMENTO);
            historicoWorkflow.setTarefaHistoricoWorkflow(TarefaWorkflow.EDITAR_DEMANDA);
            historicoWorkflow.setVersaoHistorico(historicoWorkflowAnterior.getVersaoHistorico());
            HistoricoWorkflow historicoWorkflowSalvo = historicoWorkflowService.save(historicoWorkflow);
            motivoRecusa.setIdHistoricoWorkflow(historicoWorkflowAnterior.getIdHistoricoWorkflow());
            demanda.setHistoricoWorkflowUltimaVersao(historicoWorkflowSalvo);
        }
        try {
            gerarPDFDemandaController.generatePDF(idDemanda);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Erro ao gerar PDF da demanda: " + e.getMessage());
        } finally {
            motivoRecusaService.save(motivoRecusa);
            demandaService.save(demanda);
        }
        return ResponseEntity.status(HttpStatus.OK).body("Demanda devolvida com sucesso!");
    }

}