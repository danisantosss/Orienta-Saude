import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '../hooks/useAuth';
import CartaoBeneficios from '../components/CartaoBeneficios';
import Icone from '../components/Icone';
import type { ErroResponse } from '../types';

type Campo = 'nome' | 'email' | 'senha' | 'confirmacao';

export default function Cadastro() {
  const { registrar, login } = useAuth();
  const navigate = useNavigate();
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [confirmacao, setConfirmacao] = useState('');
  const [erros, setErros] = useState<Partial<Record<Campo, string>>>({});
  const [erroGeral, setErroGeral] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

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
    if (confirmacao !== senha) {
      novosErros.confirmacao = 'As senhas não são iguais.';
    }
    return novosErros;
  }

  async function enviar(e: FormEvent) {
    e.preventDefault();
    const validacao = validar();
    if (Object.keys(validacao).length > 0) {
      setErros(validacao);
      return;
    }
    setErros({});
    setErroGeral(null);
    setEnviando(true);
    try {
      await registrar({ nome, email, senha });
      await login({ email, senha }); // backend não retorna token no registro, então logamos em seguida
      navigate('/triagem');
    } catch (err) {
      if (axios.isAxiosError<ErroResponse>(err) && err.response) {
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
      setEnviando(false);
    }
  }

  return (
    <div className="auth-page">
      <CartaoBeneficios
        titulo="Crie sua conta para iniciar sua triagem"
        texto="Com sua conta, você salva suas triagens, acompanha seu histórico e recebe orientações."
      />

      <form onSubmit={enviar} className="auth-form cartao" noValidate>
        <h1>Dados pessoais</h1>
        <p className="texto-secundario">Preencha seus dados para criar sua conta.</p>

        {erroGeral && <p role="alert" className="form-error">{erroGeral}</p>}

        <div className="grade-campos">
          <div className="campo campo-inteiro">
            <label htmlFor="nome">Nome completo</label>
            <input id="nome" value={nome} onChange={(e) => setNome(e.target.value)} autoComplete="name"
              placeholder="Digite seu nome completo" aria-invalid={Boolean(erros.nome)} />
            {erros.nome && <p className="field-error">{erros.nome}</p>}
          </div>

          <div className="campo campo-inteiro">
            <label htmlFor="email">Email</label>
            <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email"
              placeholder="Digite seu email" aria-invalid={Boolean(erros.email)} />
            {erros.email && <p className="field-error">{erros.email}</p>}
          </div>

          <div className="campo">
            <label htmlFor="senha">Senha</label>
            <input id="senha" type="password" value={senha} onChange={(e) => setSenha(e.target.value)}
              autoComplete="new-password" placeholder="Mínimo de 8 caracteres" aria-invalid={Boolean(erros.senha)} />
            {erros.senha && <p className="field-error">{erros.senha}</p>}
          </div>

          <div className="campo">
            <label htmlFor="confirmacao">Confirmar senha</label>
            <input id="confirmacao" type="password" value={confirmacao} onChange={(e) => setConfirmacao(e.target.value)}
              autoComplete="new-password" placeholder="Repita a senha" aria-invalid={Boolean(erros.confirmacao)} />
            {erros.confirmacao && <p className="field-error">{erros.confirmacao}</p>}
          </div>
        </div>

        <button type="submit" className="botao-primario botao-largo" disabled={enviando}>
          {enviando ? 'Criando conta...' : <>Criar conta e continuar <Icone nome="seta" tamanho={18} /></>}
        </button>

        <p className="auth-troca">Já tem uma conta? <Link to="/login">Entrar</Link></p>
      </form>
    </div>
  );
}
