import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '../hooks/useAuth';
import type { ApiErrorResponse } from '../types';

type Campo = 'nome' | 'email' | 'senha';

export default function Register() {
  const { register, login } = useAuth();
  const navigate = useNavigate();
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erros, setErros] = useState<Partial<Record<Campo, string>>>({});
  const [erroGeral, setErroGeral] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  function validar(): Partial<Record<Campo, string>> {
    const novosErros: Partial<Record<Campo, string>> = {};
    if (nome.trim().length < 2 || nome.trim().length > 120) {
      novosErros.nome = 'O nome deve ter entre 2 e 120 caracteres.';
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      novosErros.email = 'Email inválido.';
    }
    if (senha.length < 8 || senha.length > 128) {
      novosErros.senha = 'A senha deve ter entre 8 e 128 caracteres.';
    }
    return novosErros;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const validacao = validar();
    if (Object.keys(validacao).length > 0) {
      setErros(validacao);
      return;
    }
    setErros({});
    setErroGeral(null);
    setLoading(true);
    try {
      await register({ nome, email, senha });
      await login({ email, senha }); // backend não retorna token no registro, então logamos em seguida
      navigate('/triage');
    } catch (err) {
      if (axios.isAxiosError<ApiErrorResponse>(err) && err.response) {
        const dados = err.response.data;
        if (dados.campos) {
          setErros(dados.campos as Partial<Record<Campo, string>>);
        } else {
          setErroGeral(dados.mensagem ?? 'Não foi possível concluir o cadastro.');
        }
      } else {
        setErroGeral('Não foi possível concluir o cadastro. Tente novamente.');
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <form onSubmit={handleSubmit} className="auth-form" noValidate>
        <h1>Criar conta</h1>

        {erroGeral && <p role="alert" className="form-error">{erroGeral}</p>}

        <label htmlFor="nome">Nome</label>
        <input id="nome" value={nome} onChange={(e) => setNome(e.target.value)} autoComplete="name" />
        {erros.nome && <p className="field-error">{erros.nome}</p>}

        <label htmlFor="email">Email</label>
        <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" />
        {erros.email && <p className="field-error">{erros.email}</p>}

        <label htmlFor="senha">Senha</label>
        <input id="senha" type="password" value={senha} onChange={(e) => setSenha(e.target.value)} autoComplete="new-password" />
        {erros.senha && <p className="field-error">{erros.senha}</p>}

        <button type="submit" disabled={loading}>
          {loading ? 'Criando conta...' : 'Criar conta'}
        </button>

        <p>Já tem uma conta? <Link to="/login">Entrar</Link></p>
      </form>
    </div>
  );
}