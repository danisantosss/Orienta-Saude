import Icone, { type NomeIcone } from './Icone';

const PASSOS: { titulo: string; texto: string; icone: NomeIcone; cor: string }[] = [
  { titulo: 'Responda perguntas', texto: 'Conte seus sintomas e responda algumas perguntas simples sobre eles.', icone: 'prancheta', cor: 'verde' },
  { titulo: 'Análise com IA + RAG', texto: 'A IA analisa suas respostas e consulta uma base de conhecimento confiável.', icone: 'lupa', cor: 'azul' },
  { titulo: 'Resultado personalizado', texto: 'Você recebe possíveis condições relacionadas e o nível de urgência estimado.', icone: 'grafico', cor: 'lilas' },
  { titulo: 'Orientações educativas', texto: 'Veja recomendações claras sobre o que fazer e quando procurar ajuda.', icone: 'escudo', cor: 'amarelo' },
];

/** Faixa "Como funciona?" com os quatro passos do serviço, mostrada no rodapé de todas as páginas. */
export default function ComoFunciona() {
  return (
    <section id="como-funciona" className="como-funciona" aria-labelledby="titulo-como-funciona">
      <h2 id="titulo-como-funciona">Como funciona?</h2>
      <p className="texto-secundario">Um processo simples para você entender seus sintomas e receber orientações.</p>
      <ol>
        {PASSOS.map((passo, indice) => (
          <li key={passo.titulo} className={`passo passo-${passo.cor}`}>
            <span className="passo-icone"><Icone nome={passo.icone} /></span>
            <div>
              <h3><span className="passo-numero" aria-hidden="true">{indice + 1}</span>{passo.titulo}</h3>
              <p>{passo.texto}</p>
            </div>
          </li>
        ))}
      </ol>
    </section>
  );
}
