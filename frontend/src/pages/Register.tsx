import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '../hooks/useAuth';
import type { ApiErrorResponse } from '../types';

type Field = 'name' | 'email' | 'password';

export default function Register() {
  const { register, login } = useAuth();
  const navigate = useNavigate();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<Partial<Record<Field, string>>>({});
  const [generalError, setGeneralError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  function validate(): Partial<Record<Field, string>> {
    const newErrors: Partial<Record<Field, string>> = {};
    if (name.trim().length < 2 || name.trim().length > 120) {
      newErrors.name = 'O nome deve ter entre 2 e 120 caracteres.';
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      newErrors.email = 'Email inválido.';
    }
    if (password.length < 8 || password.length > 128) {
      newErrors.password = 'A senha deve ter entre 8 e 128 caracteres.';
    }
    return newErrors;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const validation = validate();
    if (Object.keys(validation).length > 0) {
      setErrors(validation);
      return;
    }
    setErrors({});
    setGeneralError(null);
    setLoading(true);
    try {
      await register({ name, email, password });
      await login({ email, password }); // the backend does not return a token on registration, so we log in right after
      navigate('/triage');
    } catch (err) {
      if (axios.isAxiosError<ApiErrorResponse>(err) && err.response) {
        const data = err.response.data;
        if (data.fields) {
          setErrors(data.fields as Partial<Record<Field, string>>);
        } else {
          setGeneralError(data.message ?? 'Não foi possível concluir o cadastro.');
        }
      } else {
        setGeneralError('Não foi possível concluir o cadastro. Tente novamente.');
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <form onSubmit={handleSubmit} className="auth-form" noValidate>
        <h1>Criar conta</h1>

        {generalError && <p role="alert" className="form-error">{generalError}</p>}

        <label htmlFor="name">Nome</label>
        <input id="name" value={name} onChange={(e) => setName(e.target.value)} autoComplete="name" />
        {errors.name && <p className="field-error">{errors.name}</p>}

        <label htmlFor="email">Email</label>
        <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" />
        {errors.email && <p className="field-error">{errors.email}</p>}

        <label htmlFor="password">Senha</label>
        <input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="new-password" />
        {errors.password && <p className="field-error">{errors.password}</p>}

        <button type="submit" disabled={loading}>
          {loading ? 'Criando conta...' : 'Criar conta'}
        </button>

        <p>Já tem uma conta? <Link to="/login">Entrar</Link></p>
      </form>
    </div>
  );
}
