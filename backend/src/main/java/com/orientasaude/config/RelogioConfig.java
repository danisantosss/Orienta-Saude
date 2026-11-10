package com.orientasaude.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Relógio da aplicação. Os componentes que dependem de tempo (bloqueio de login, limite de
 * requisições, retenção da auditoria) recebem o Clock por injeção, e os testes trocam por um
 * relógio fixo para simular a passagem do tempo sem esperar de verdade.
 * Também liga as tarefas agendadas (@Scheduled).
 */
@Configuration
@EnableScheduling
public class RelogioConfig {

    @Bean
    public Clock relogio() {
        return Clock.systemDefaultZone();
    }
}
