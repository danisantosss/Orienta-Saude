interface IndicadorCarregamentoProps {
  mensagem: string;
  demorando?: boolean;
}

/** Indicador de carregamento acessível, com aviso de demora depois de 10 s. */
export default function IndicadorCarregamento({ mensagem, demorando = false }: IndicadorCarregamentoProps) {
  return (
    <div className="carregamento" role="status" aria-live="polite">
      <span className="carregamento-roda" aria-hidden="true" />
      <p>{mensagem}</p>
      {demorando && <p className="carregamento-demora">Isso está demorando mais que o esperado. Aguarde mais um pouco.</p>}
    </div>
  );
}
