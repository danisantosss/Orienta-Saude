import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '../hooks/useAuth';
import type { ApiErrorResponse } from '../types';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  function validar(): string | null {
    if (!email.trim()) return 'Informe o email.';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return 'Email inválido.';
    if (!senha) return 'Informe a senha.';
    return null;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const erroValidacao = validar();
    if (erroValidacao) {
      setErro(erroValidacao);
      return;
    }
    setErro(null);
    setLoading(true);
    try {
      await login({ email, senha });
      navigate('/triage');
    } catch (err) {
      if (axios.isAxiosError<ApiErrorResponse>(err) && err.response) {
        setErro(err.response.data.mensagem ?? 'Não foi possível entrar.');
      } else {
        setErro('Não foi possível entrar. Tente novamente.');
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <form onSubmit={handleSubmit} className="auth-form" noValidate>
        <h1>Entrar</h1>

        {erro && <p role="alert" className="form-error">{erro}</p>}

        <label htmlFor="email">Email</label>
        <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" />

        <label htmlFor="senha">Senha</label>
        <input id="senha" type="password" value={senha} onChange={(e) => setSenha(e.target.value)} autoComplete="current-password" />

        <button type="submit" disabled={loading}>
          {loading ? 'Entrando...' : 'Entrar'}
        </button>

        <p>Não tem uma conta? <Link to="/register">Cadastre-se</Link></p>
      </form>
    </div>
  );
}