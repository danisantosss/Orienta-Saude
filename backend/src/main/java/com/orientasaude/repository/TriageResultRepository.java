package com.orientasaude.repository;

import com.orientasaude.model.TriageResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TriageResultRepository extends JpaRepository<TriageResult, Long> {

    Optional<TriageResult> findBySessionId(Long sessionId);
}
