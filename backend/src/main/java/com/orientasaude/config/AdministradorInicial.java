package com.orientasaude.config;

import com.orientasaude.model.Usuario;
import com.orientasaude.model.enums.PapelUsuario;
import com.orientasaude.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Dá o papel ADMIN, ao subir a aplicação, às contas cujos emails estão em app.admin.emails
 * (variável APP_ADMIN_EMAILS, separada por vírgula). A conta precisa já existir: a pessoa se
 * cadastra normalmente e quem implanta coloca o email dela na variável.
 *
 * É a única forma, além de um UPDATE direto no banco, de alguém virar administrador:
 * o cadastro público sempre cria USUARIO. Tirar o email da variável não rebaixa a conta;
 * para isso, volte o papel para USUARIO no banco.
 */
@Component
public class AdministradorInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministradorInicial.class);

    private final UsuarioRepository usuarioRepository;
    private final List<String> emails;

    public AdministradorInicial(UsuarioRepository usuarioRepository,
                                @Value("${app.admin.emails:}") String emails) {
        this.usuarioRepository = usuarioRepository;
        this.emails = Arrays.stream(emails.split(","))
                .map(email -> email.trim().toLowerCase())
                .filter(email -> !email.isEmpty())
                .toList();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int promovidas = 0;
        for (String email : emails) {
            Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
            if (usuario != null && usuario.getPapel() != PapelUsuario.ADMIN) {
                usuario.setPapel(PapelUsuario.ADMIN);
                usuarioRepository.save(usuario);
                promovidas++;
            }
        }
        if (!emails.isEmpty()) {
            // Só a contagem vai para o log: o email é dado pessoal
            log.info("Papel ADMIN conferido para {} email(s) configurado(s); {} conta(s) promovida(s) agora",
                    emails.size(), promovidas);
        }
    }
}
