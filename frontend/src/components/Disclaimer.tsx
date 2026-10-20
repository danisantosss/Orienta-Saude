export const TEXTO_DISCLAIMER =
  'O Orienta Saúde é um serviço exclusivamente educativo: não realiza diagnóstico e não substitui a avaliação ' +
  'de um profissional de saúde. Em caso de emergência, ligue para o SAMU (192).';

interface DisclaimerProps {
  texto?: string;
  confirmado?: boolean;
  aoConfirmar?: (confirmado: boolean) => void;
}

/**
 * Aviso de que o serviço é educativo (Requisito 14). Com `aoConfirmar`, exige que o usuário
 * marque que leu antes de continuar (14.2).
 */
export default function Disclaimer({ texto = TEXTO_DISCLAIMER, confirmado = false, aoConfirmar }: DisclaimerProps) {
  return (
    <div className="disclaimer" role="note" aria-label="Aviso importante">
      <p>
        <span aria-hidden="true" className="disclaimer-icone">⚠️ </span>
        {texto}
      </p>
      {aoConfirmar && (
        <label className="disclaimer-confirmacao">
          <input type="checkbox" checked={confirmado} onChange={(e) => aoConfirmar(e.target.checked)} />
          Li e entendi que as orientações são educativas
        </label>
      )}
    </div>
  );
}
