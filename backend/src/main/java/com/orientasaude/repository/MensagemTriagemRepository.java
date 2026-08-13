package com.orientasaude.repository;

import com.orientasaude.model.MensagemTriagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensagemTriagemRepository extends JpaRepository<MensagemTriagem, Long> {

    List<MensagemTriagem> findBySessaoIdOrderByOrdemSequenciaAsc(Long sessaoId);

    long countBySessaoId(Long sessaoId);
}
