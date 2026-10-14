package com.orientasaude.repository;

import com.orientasaude.model.TriageMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TriageMessageRepository extends JpaRepository<TriageMessage, Long> {

    List<TriageMessage> findBySessionIdOrderBySequenceOrderAsc(Long sessionId);

    long countBySessionId(Long sessionId);
}
