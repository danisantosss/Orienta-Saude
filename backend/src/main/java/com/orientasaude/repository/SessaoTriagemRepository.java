package com.orientasaude.repository;

import com.orientasaude.model.SessaoTriagem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessaoTriagemRepository extends JpaRepository<SessaoTriagem, Long> {

    Page<SessaoTriagem> findByUsuarioIdOrderByIniciadaEmDesc(Long usuarioId, Pageable pageable);
}
