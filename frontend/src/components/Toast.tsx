import { useEffect } from 'react';

/** Tempo que a mensagem de sucesso fica visível (Requisito 10.3). */
export const DURACAO_TOAST_MS = 3000;

interface ToastProps {
  mensagem: string;
  aoFechar: () => void;
}

export default function Toast({ mensagem, aoFechar }: ToastProps) {
  useEffect(() => {
    const temporizador = setTimeout(aoFechar, DURACAO_TOAST_MS);
    return () => clearTimeout(temporizador);
  }, [aoFechar]);

  return (
    <div className="toast" role="status" aria-live="polite">
      {mensagem}
    </div>
  );
}
