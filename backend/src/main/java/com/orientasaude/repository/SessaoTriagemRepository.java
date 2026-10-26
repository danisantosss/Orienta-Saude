package com.orientasaude.repository;

import com.orientasaude.model.SessaoTriagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SessaoTriagemRepository extends JpaRepository<SessaoTriagem, Long>,
        JpaSpecificationExecutor<SessaoTriagem> {
}
