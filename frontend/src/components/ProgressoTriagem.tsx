const ETAPAS = [
  { titulo: 'Sintomas principais', descricao: 'Conte o que você está sentindo' },
  { titulo: 'Detalhe dos sintomas', descricao: 'Responda algumas perguntas' },
  { titulo: 'Resultado', descricao: 'Receba sua análise e orientações' },
];

/** Etapas da triagem (cartão "Seu progresso" do protótipo). */
export default function ProgressoTriagem({ etapaAtual }: { etapaAtual: 1 | 2 | 3 }) {
  return (
    <aside className="cartao progresso" aria-label="Seu progresso">
      <h2>Seu progresso</h2>
      <ol>
        {ETAPAS.map((etapa, indice) => {
          const numero = indice + 1;
          const classe = numero < etapaAtual ? 'etapa-concluida' : numero === etapaAtual ? 'etapa-atual' : '';
          return (
            <li key={etapa.titulo} className={`etapa ${classe}`} aria-current={numero === etapaAtual ? 'step' : undefined}>
              <span className="etapa-numero" aria-hidden="true">{numero < etapaAtual ? '✓' : numero}</span>
              <span>
                <strong>{etapa.titulo}</strong>
                <span className="etapa-descricao">{etapa.descricao}</span>
              </span>
            </li>
          );
        })}
      </ol>
    </aside>
  );
}
