import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import Icone from '../components/Icone';
import Ilustracao from '../components/Ilustracao';

/** Página inicial: apresenta o serviço e leva para a triagem (ou para o login, se a pessoa não entrou). */
export default function Home() {
  const { autenticado } = useAuth();
  return (
    <div className="pagina-home">
      <section className="hero cartao" aria-labelledby="titulo-home">
        <div className="hero-texto">
          <h1 id="titulo-home">Entenda seus sintomas. Receba orientações com apoio de Inteligência Artificial.</h1>
          <p>
            O Orienta Saúde usa Inteligência Artificial e uma base de conhecimento em saúde para ajudar você a
            entender possíveis condições relacionadas aos seus sintomas e a saber quando procurar atendimento.
          </p>
          <p className="aviso-educativo">
            <Icone nome="escudo" tamanho={20} />
            Este serviço é informativo e educativo. Não substitui a avaliação médica profissional.
          </p>
          <div className="hero-acoes">
            <Link to={autenticado ? '/triagem' : '/login'} className="botao-primario">
              {autenticado ? 'Iniciar triagem' : 'Entrar para começar'} <Icone nome="seta" tamanho={18} />
            </Link>
            <a href="#como-funciona" className="botao-secundario">Saiba mais</a>
          </div>
        </div>
        <Ilustracao variante="celular" className="hero-ilustracao" />
      </section>
    </div>
  );
}
