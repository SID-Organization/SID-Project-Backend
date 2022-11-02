package br.sc.weg.sid.model.entities;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "BENEFICIO")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Beneficio {
    @Id
    @JoinColumn(name = "idBeneficio", nullable = false)
    private Integer idBeneficio;

    @Enumerated(EnumType.STRING)
    @Column(name = "MoedaBeneficio", nullable = false)
    private Moeda moedaBeneficio;

    @Column(name = "MemoriaCalculoBeneficio", nullable = false)
    private Double memoriaCalculoBeneficio;

    @Column(length = 4000, name = "DescricaoBeneficio", nullable = false)
    private String descricaoBeneficio;

    @ManyToOne
    @JoinColumn(name = "idDemanda", nullable = false)
    private Demanda idDemanda;
}
