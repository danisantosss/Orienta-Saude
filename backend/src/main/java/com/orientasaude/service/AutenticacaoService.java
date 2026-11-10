package com.orientasaude.service;

import com.orientasaude.dto.request.LoginRequest;
import com.orientasaude.dto.request.RegistroRequest;
import com.orientasaude.dto.response.AutenticacaoResponse;
import com.orientasaude.exception.EmailJaCadastradoException;
import com.orientasaude.model.Usuario;
import com.orientasaude.model.enums.TipoEventoAuditoria;
import com.orientasaude.repository.UsuarioRepository;
import com.orientasaude.security.BloqueioLoginService;
import com.orientasaude.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Serviço responsável pelo cadastro e autenticação de usuários.
 */
@Service
@RequiredArgsConstructor
public class AutenticacaoService {

    /** Versão da Política de Privacidade exibida em /privacidade. Mudou o texto, muda a versão. */
    public static final String VERSAO_POLITICA_PRIVACIDADE = "1.0";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final BloqueioLoginService bloqueioLoginService;
    private final AuditoriaService auditoriaService;

    /**
     * Registra um novo usuário no sistema.
     * Valida unicidade do email, armazena a senha com BCrypt e registra quando e qual versão
     * da política de privacidade foi aceita (o aceite em si já foi exigido na validação do DTO).
     */
    @Transactional
    public Usuario registrar(RegistroRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new EmailJaCadastradoException(request.getEmail());
        }

        Usuario usuario = Usuario.builder()
                .nome(request.getNome().trim())
                .email(request.getEmail().trim().toLowerCase())
                .senhaHash(passwordEncoder.encode(request.getSenha()))
                .ativo(true)
                .consentimentoEm(LocalDateTime.now())
                .versaoPolitica(VERSAO_POLITICA_PRIVACIDADE)
                .build();

        return usuarioRepository.save(usuario);
    }

    /**
     * Autentica um usuário e retorna o token JWT.
     * Retorna mensagem genérica se credenciais inválidas (não revela se email existe).
     * Cada falha vai para a auditoria e conta para o bloqueio do IP; com o IP bloqueado,
     * nem a senha certa entra até o bloqueio vencer.
     */
    public AutenticacaoResponse login(LoginRequest request, String ip) {
        bloqueioLoginService.verificar(ip);

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail().trim().toLowerCase()).orElse(null);
        boolean credenciaisValidas = usuario != null
                && Boolean.TRUE.equals(usuario.getAtivo())
                && passwordEncoder.matches(request.getSenha(), usuario.getSenhaHash());

        if (!credenciaisValidas) {
            registrarFalha(usuario, ip);
            throw new BadCredentialsException("Credenciais inválidas");
        }

        bloqueioLoginService.registrarSucesso(ip);
        String token = jwtTokenProvider.gerarToken(usuario.getId(), usuario.getEmail(), usuario.getPapel());

        return new AutenticacaoResponse(token, jwtTokenProvider.getExpiracaoMs());
    }

    private void registrarFalha(Usuario usuario, String ip) {
        auditoriaService.registrar(TipoEventoAuditoria.LOGIN_FALHO, usuario != null ? usuario.getId() : null, ip);
        if (bloqueioLoginService.registrarFalha(ip)) {
            auditoriaService.registrar(TipoEventoAuditoria.LOGIN_BLOQUEADO, null, ip);
        }
    }
}
