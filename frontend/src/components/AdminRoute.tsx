import { Link, Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

/**
 * Rotas só para administradores. Sem login vai para /login; logado sem o papel ADMIN vê um aviso.
 * É só a parte visual: mesmo que alguém force a rota, o backend responde 403 aos dados.
 */
export default function AdminRoute() {
  const { autenticado, ehAdmin, carregando } = useAuth();

  if (carregando) {
    return <p>Carregando...</p>;
  }

  if (!autenticado) {
    return <Navigate to="/login" replace />;
  }

  if (!ehAdmin) {
    return (
      <div className="cartao acesso-restrito">
        <h1>Acesso restrito</h1>
        <p role="alert">Esta área é só para administradores da plataforma.</p>
        <Link to="/" className="botao-primario">Voltar para o início</Link>
      </div>
    );
  }

  return <Outlet />;
}
