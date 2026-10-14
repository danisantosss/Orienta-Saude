import { Outlet, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export default function Layout() {
  const { autenticado, logout } = useAuth();
  const navigate = useNavigate();

  function sair() {
    logout();
    navigate('/login');
  }

  return (
    <div className="layout">
      <header className="layout-header">
        <Link to="/" className="logo">Orienta Saúde</Link>
        <nav>
          {autenticado ? (
            <>
              <Link to="/triagem">Nova triagem</Link>
              <Link to="/historico">Histórico</Link>
              <button onClick={sair}>Sair</button>
            </>
          ) : (
            <>
              <Link to="/login">Entrar</Link>
              <Link to="/cadastro">Cadastrar</Link>
            </>
          )}
        </nav>
      </header>
      <main className="layout-content">
        <Outlet />
      </main>
    </div>
  );
}