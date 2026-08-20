package com.orientasaude.service;

import com.orientasaude.dto.request.LoginRequest;
import com.orientasaude.dto.request.RegistroRequest;
import com.orientasaude.dto.response.AutenticacaoResponse;
import com.orientasaude.exception.EmailJaCadastradoException;
import com.orientasaude.model.Usuario;
import com.orientasaude.repository.UsuarioRepository;
import com.orientasaude.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável pelo cadastro e autenticação de usuários.
 */
@Service
@RequiredArgsConstructor
public class AutenticacaoService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Registra um novo usuário no sistema.
     * Valida unicidade do email e armazena a senha com BCrypt.
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
                .build();

        return usuarioRepository.save(usuario);
    }

    /**
     * Autentica um usuário e retorna o token JWT.
     * Retorna mensagem genérica se credenciais inválidas (não revela se email existe).
     */
    public AutenticacaoResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

        if (!usuario.getAtivo()) {
            throw new BadCredentialsException("Credenciais inválidas");
        }

        if (!passwordEncoder.matches(request.getSenha(), usuario.getSenhaHash())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }

        String token = jwtTokenProvider.gerarToken(usuario.getId(), usuario.getEmail());

        return new AutenticacaoResponse(token, jwtTokenProvider.getExpiracaoMs());
    }
}
