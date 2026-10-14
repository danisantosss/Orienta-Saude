import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export default function Home() {
  const { autenticado } = useAuth();
  return (
    <div>
      <h1>Orienta Saúde</h1>
      <p>Plataforma de triagem inicial de sintomas.</p>
      {autenticado ? (
        <Link to="/triagem">Iniciar nova triagem</Link>
      ) : (
        <Link to="/login">Entrar para começar</Link>
      )}
    </div>
  );
}