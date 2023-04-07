package br.sc.weg.sid.repository;

import br.sc.weg.sid.model.entities.Proposta;
import br.sc.weg.sid.model.entities.TabelaCusto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TabelaCustoRepository extends JpaRepository<TabelaCusto, Integer> {

    public void deleteByProposta(Proposta proposta);

    public List<TabelaCusto> findByProposta(Proposta proposta);
}
