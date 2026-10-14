package com.orientasaude.repository;

import com.orientasaude.model.TriageSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TriageSessionRepository extends JpaRepository<TriageSession, Long> {

    Page<TriageSession> findByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);
}
