import { FormEvent, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { excluirConta, obterPerfil } from '../api/usuario';
import { useAuth } from '../hooks/useAuth';
import Icone from '../components/Icone';
import IndicadorCarregamento from '../components/IndicadorCarregamento';
import type { ErroResponse, PerfilUsuario } from '../types';

function formatarDia(data: string): string {
  return new Date(data).toLocaleDateString('pt-BR');
}

function formatarDiaEHora(data: string): string {
  const quando = new Date(data);
  const hora = quando.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
  return `${formatarDia(data)} às ${hora}`;
}

/**
 * Meu perfil (rota /perfil): mostra os dados da conta e o registro do consentimento,
 * e permite excluir a conta com todos os dados, depois de confirmar a senha.
 */
export default function Perfil() {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [perfil, setPerfil] = useState<PerfilUsuario | null>(null);
  const [erroCarregar, setErroCarregar] = useState(false);
  const [confirmando, setConfirmando] = useState(false);
  const [senha, setSenha] = useState('');
  const [erroExclusao, setErroExclusao] = useState<string | null>(null);
  const [excluindo, setExcluindo] = useState(false);

  useEffect(() => {
    let ativo = true;
    obterPerfil()
      .then((dados) => { if (ativo) setPerfil(dados); })
      .catch(() => { if (ativo) setErroCarregar(true); });
    return () => { ativo = false; };
  }, []);

  function cancelar() {
    setConfirmando(false);
    setSenha('');
    setErroExclusao(null);
  }

  async function confirmarExclusao(e: FormEvent) {
    e.preventDefault();
    if (!senha) {
      setErroExclusao('Informe sua senha para confirmar.');
      return;
    }
    setErroExclusao(null);
    setExcluindo(true);
    try {
      const mensagem = await excluirConta(senha);
      logout();
      navigate('/login', { replace: true, state: { aviso: mensagem } });
    } catch (err) {
      if (axios.isAxiosError<ErroResponse>(err) && err.response?.data?.mensagem) {
        setErroExclusao(err.response.data.mensagem);
      } else {
        setErroExclusao('Não foi possível excluir a conta. Tente novamente.');
      }
      setExcluindo(false);
    }
  }

  if (erroCarregar) {
    return <p role="alert" className="form-error">Não foi possível carregar seus dados.</p>;
  }
  if (!perfil) {
    return <IndicadorCarregamento mensagem="Carregando seus dados..." />;
  }

  return (
    <div className="pagina-perfil">
      <section className="cartao">
        <h1>Meu perfil</h1>
        <dl className="dados-perfil">
          <dt>Nome</dt>
          <dd>{perfil.nome}</dd>
          <dt>Email</dt>
          <dd>{perfil.email}</dd>
          <dt>Cadastro em</dt>
          <dd>{formatarDia(perfil.criadoEm)}</dd>
          <dt>Consentimento</dt>
          <dd>
            {perfil.consentimentoEm
              ? `Aceito em ${formatarDiaEHora(perfil.consentimentoEm)} (versão ${perfil.versaoPolitica})`
              : 'Não registrado (conta criada antes da política)'}
          </dd>
        </dl>
        <Link to="/privacidade">Ler a Política de Privacidade</Link>
      </section>

      <section className="cartao zona-exclusao">
        <h2>Excluir conta</h2>
        <p>
          Apaga sua conta, todas as triagens, mensagens e resultados. Isso não pode ser desfeito
          e o login com este email deixa de funcionar.
        </p>

        {!confirmando ? (
          <button type="button" className="botao-perigo" onClick={() => setConfirmando(true)}>
            <Icone nome="lixeira" tamanho={18} /> Excluir minha conta
          </button>
        ) : (
          <form onSubmit={confirmarExclusao} noValidate className="form-exclusao">
            <div className="campo">
              <label htmlFor="senha-exclusao">Senha</label>
              <input id="senha-exclusao" type="password" value={senha} onChange={(e) => setSenha(e.target.value)}
                autoComplete="current-password" placeholder="Digite sua senha para confirmar"
                aria-invalid={Boolean(erroExclusao)} />
              {erroExclusao && <p className="field-error">{erroExclusao}</p>}
            </div>
            <div className="acoes-exclusao">
              <button type="button" className="botao-secundario" onClick={cancelar} disabled={excluindo}>
                Cancelar
              </button>
              <button type="submit" className="botao-perigo" disabled={excluindo}>
                {excluindo ? 'Excluindo...' : 'Confirmar exclusão'}
              </button>
            </div>
          </form>
        )}
      </section>
    </div>
  );
}
