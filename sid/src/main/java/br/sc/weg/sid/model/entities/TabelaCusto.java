package br.sc.weg.sid.model.entities;

import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "TABELA_CUSTO")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Data
public class TabelaCusto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IdTabelaCusto", nullable = false, unique = true)
    private Integer idTabelaCusto;

    @Column(name = "TipoDespesa", nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoDeDespesa tipoDespesa;

    @Column(name = "PerfilDespesaTabelaCusto", nullable = false)
    private String perfilDespesaTabelaCusto;

    @Column(name = "PeriodoExecucaoTabelaCusto", nullable = false)
    private Integer periodoExecucaoTabelaCusto;

    @Column(name = "QuantidadeHorasTabelaCusto", nullable = false)
    private Integer quantidadeHorasTabelaCusto;

    @Column(name = "ValorHoraTabelaCusto", nullable = false)
    private Double valorHoraTabelaCusto;

    @JoinColumn(name = "IdProposta", nullable = false)
    @ManyToOne
    private Proposta proposta;
}
