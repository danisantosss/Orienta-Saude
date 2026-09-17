import { Outlet, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export default function Layout() {
  const { isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <div className="layout">
      <header className="layout-header">
        <Link to="/" className="logo">Orienta Saúde</Link>
        <nav>
          {isAuthenticated ? (
            <>
              <Link to="/triage">Nova triagem</Link>
              <Link to="/history">Histórico</Link>
              <button onClick={handleLogout}>Sair</button>
            </>
          ) : (
            <>
              <Link to="/login">Entrar</Link>
              <Link to="/register">Cadastrar</Link>
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