import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '../hooks/useAuth';
import CartaoBeneficios from '../components/CartaoBeneficios';
import Icone from '../components/Icone';
import type { ErroResponse } from '../types';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  function validar(): string | null {
    if (!email.trim()) return 'Informe o email.';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return 'Email inválido.';
    if (!senha) return 'Informe a senha.';
    return null;
  }

  async function enviar(e: FormEvent) {
    e.preventDefault();
    const erroValidacao = validar();
    if (erroValidacao) {
      setErro(erroValidacao);
      return;
    }
    setErro(null);
    setEnviando(true);
    try {
      await login({ email, senha });
      navigate('/triagem');
    } catch (err) {
      if (axios.isAxiosError<ErroResponse>(err) && err.response) {
        setErro(err.response.data.mensagem ?? 'Não foi possível entrar.');
      } else {
        setErro('Não foi possível entrar. Tente novamente.');
      }
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="auth-page">
      <CartaoBeneficios
        titulo="Entre para continuar sua triagem"
        texto="Com sua conta, você salva suas triagens, acompanha seu histórico e recebe orientações."
      />

      <form onSubmit={enviar} className="auth-form cartao" noValidate>
        <h1>Entrar</h1>
        <p className="texto-secundario">Use o email e a senha que você cadastrou.</p>

        {erro && <p role="alert" className="form-error">{erro}</p>}

        <div className="campo">
          <label htmlFor="email">Email</label>
          <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)}
            autoComplete="email" placeholder="Digite seu email" />
        </div>

        <div className="campo">
          <label htmlFor="senha">Senha</label>
          <input id="senha" type="password" value={senha} onChange={(e) => setSenha(e.target.value)}
            autoComplete="current-password" placeholder="Digite sua senha" />
        </div>

        <button type="submit" className="botao-primario botao-largo" disabled={enviando}>
          {enviando ? 'Entrando...' : <>Entrar <Icone nome="seta" tamanho={18} /></>}
        </button>

        <p className="auth-troca">Não tem uma conta? <Link to="/cadastro">Cadastre-se</Link></p>
      </form>
    </div>
  );
}
