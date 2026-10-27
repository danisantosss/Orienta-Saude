import { useEffect, useState } from 'react';
import { Outlet, Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import ComoFunciona from './ComoFunciona';
import Icone from './Icone';

export default function Layout() {
  const { autenticado, usuario, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [menuAberto, setMenuAberto] = useState(false);

  // No celular o menu fecha sozinho quando o usuário troca de página
  useEffect(() => { setMenuAberto(false); }, [location.pathname]);

  function sair() {
    logout();
    navigate('/login');
  }

  return (
    <div className="layout">
      <header className="layout-header">
        <Link to="/" className="logo">
          <Icone nome="coracao" tamanho={28} className="logo-icone" />
          Orienta Saúde
        </Link>

        <button
          type="button"
          className="menu-botao"
          aria-expanded={menuAberto}
          aria-controls="menu-principal"
          onClick={() => setMenuAberto((aberto) => !aberto)}
        >
          <Icone nome={menuAberto ? 'fechar' : 'menu'} />
          <span className="somente-leitor">{menuAberto ? 'Fechar menu' : 'Abrir menu'}</span>
        </button>

        <div id="menu-principal" className={`menu ${menuAberto ? 'menu-aberto' : ''}`}>
          <nav aria-label="Principal" className="menu-links">
            <NavLink to="/" end>Início</NavLink>
            <a href="#como-funciona" onClick={() => setMenuAberto(false)}>Como funciona</a>
            {autenticado && (
              <>
                <NavLink to="/triagem">Nova triagem</NavLink>
                <NavLink to="/historico">Histórico</NavLink>
              </>
            )}
          </nav>

          <div className="menu-conta">
            {autenticado ? (
              <>
                <span className="conta-usuario">
                  <Icone nome="usuario" tamanho={28} />
                  <span className="conta-email">{usuario?.email}</span>
                </span>
                <button type="button" className="botao-secundario botao-pequeno" onClick={sair}>
                  <Icone nome="sair" tamanho={18} /> Sair
                </button>
              </>
            ) : (
              <>
                <Link to="/cadastro" className="link-cadastro">Cadastrar</Link>
                <Link to="/login" className="botao-primario botao-pequeno">Entrar</Link>
              </>
            )}
          </div>
        </div>
      </header>

      <main className="layout-content">
        <Outlet />
      </main>

      <footer className="layout-rodape">
        <ComoFunciona />
        <p className="rodape-final">
          Orienta Saúde é um serviço educativo e não substitui a avaliação de um profissional.
          Em caso de emergência, ligue para o <strong>SAMU (192)</strong>.
        </p>
      </footer>
    </div>
  );
}
