import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export default function PrivateRoute() {
  const { autenticado, carregando } = useAuth();

  if (carregando) {
    return <p>Carregando...</p>;
  }

  return autenticado ? <Outlet /> : <Navigate to="/login" replace />;
}