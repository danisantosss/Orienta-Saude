package com.orientasaude.repository;

import com.orientasaude.model.ResultadoTriagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResultadoTriagemRepository extends JpaRepository<ResultadoTriagem, Long> {

    Optional<ResultadoTriagem> findBySessaoId(Long sessaoId);
}
