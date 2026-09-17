import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export default function Home() {
  const { isAuthenticated } = useAuth();
  return (
    <div>
      <h1>Orienta Saúde</h1>
      <p>Plataforma de triagem inicial de sintomas.</p>
      {isAuthenticated ? (
        <Link to="/triage">Iniciar nova triagem</Link>
      ) : (
        <Link to="/login">Entrar para começar</Link>
      )}
    </div>
  );
}