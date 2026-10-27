import Icone, { type NomeIcone } from './Icone';
import Ilustracao from './Ilustracao';

const BENEFICIOS: { titulo: string; texto: string; icone: NomeIcone }[] = [
  { titulo: 'Seus dados protegidos', texto: 'Seu nome e email nunca são enviados para a IA.', icone: 'cadeado' },
  { titulo: 'Histórico das triagens', texto: 'Consulte quando quiser as triagens que você já fez.', icone: 'historico' },
  { titulo: 'Orientações educativas', texto: 'Saiba o que fazer e quando procurar atendimento.', icone: 'escudo' },
];

/** Cartão ao lado dos formulários de login e cadastro, com o que a pessoa ganha ao ter uma conta. */
export default function CartaoBeneficios({ titulo, texto }: { titulo: string; texto: string }) {
  return (
    <aside className="cartao cartao-beneficios">
      <h2>{titulo}</h2>
      <p>{texto}</p>
      <ul>
        {BENEFICIOS.map((beneficio) => (
          <li key={beneficio.titulo}>
            <Icone nome={beneficio.icone} tamanho={26} />
            <div>
              <strong>{beneficio.titulo}</strong>
              <span>{beneficio.texto}</span>
            </div>
          </li>
        ))}
      </ul>
      <Ilustracao variante="cuidado" className="beneficios-ilustracao" />
    </aside>
  );
}
