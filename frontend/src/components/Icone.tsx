import type { ReactNode } from 'react';

/** Traços de cada ícone, desenhados numa grade de 24×24 com linha de 2 px. */
const TRACOS = {
  coracao: <path d="M12 20s-7-4.4-9.2-9A5 5 0 0 1 12 6a5 5 0 0 1 9.2 5C19 15.6 12 20 12 20z" />,
  usuario: <><circle cx="12" cy="12" r="10" /><circle cx="12" cy="10" r="3" /><path d="M6.2 18.4a7 7 0 0 1 11.6 0" /></>,
  menu: <path d="M4 6h16M4 12h16M4 18h16" />,
  fechar: <path d="M6 6l12 12M18 6 6 18" />,
  seta: <path d="M5 12h14M13 6l6 6-6 6" />,
  cadeado: <><rect x="5" y="11" width="14" height="10" rx="2" /><path d="M8 11V8a4 4 0 0 1 8 0v3" /></>,
  escudo: <><path d="M12 3 4 6v6c0 4.4 3.4 7.8 8 9 4.6-1.2 8-4.6 8-9V6z" /><path d="m9 12 2 2 4-4" /></>,
  prancheta: <><rect x="6" y="4" width="12" height="17" rx="2" /><path d="M9 4h6v3H9zM9 11h6M9 15h4" /></>,
  lupa: <><circle cx="11" cy="11" r="6" /><path d="m20 20-4.5-4.5" /></>,
  grafico: <path d="M5 20V12M10 20V8M15 20v-5M20 20V5" />,
  historico: <><path d="M3 12a9 9 0 1 0 3-6.7L3 8" /><path d="M3 3v5h5M12 7v5l3 2" /></>,
  sair: <><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" /><path d="M16 17l5-5-5-5M21 12H9" /></>,
  termometro: <><path d="M14 14.8V5a2 2 0 0 0-4 0v9.8a4 4 0 1 0 4 0z" /><path d="M12 17v-5" /></>,
  raio: <path d="M13 3 5 14h6l-1 7 8-11h-6z" />,
  vento: <path d="M3 8h11a3 3 0 1 0-3-3M3 12h15a3 3 0 1 1-3 3M3 16h7" />,
  gota: <path d="M12 3s-6 6.6-6 11a6 6 0 0 0 12 0c0-4.4-6-11-6-11z" />,
  estomago: <path d="M9 3v4c0 2-3 3-3 7a6 6 0 0 0 6 6c4 0 6-3 6-6 0-2-1-3-2.5-3S13 12 12 12s-2-1-2-3V3" />,
  osso: <path d="M8 6.5a2.5 2.5 0 1 0-3 3l9.5 9.5a2.5 2.5 0 1 0 3-3L8 6.5zM15.5 5a2.5 2.5 0 1 1 3.5 3.5M5 15.5A2.5 2.5 0 1 0 8.5 19" />,
  pulmao: <><path d="M12 4v8M12 12c-1 0-2 1-3 2" /><path d="M8 7C5 7 3 12 3 17c0 2 1 3 3 3 3 0 4-2 4-5V9a2 2 0 0 0-2-2zM16 7c3 0 5 5 5 10 0 2-1 3-3 3-3 0-4-2-4-5V9a2 2 0 0 1 2-2z" /></>,
  onda: <path d="M3 9c3-3 6 3 9 0s6 3 9 0M3 15c3-3 6 3 9 0s6 3 9 0" />,
  virus: <><circle cx="12" cy="12" r="5.5" /><path d="M12 3.5v3M12 17.5v3M3.5 12h3M17.5 12h3M6 6l2.1 2.1M15.9 15.9 18 18M6 18l2.1-2.1M15.9 8.1 18 6" /><path d="M10.5 10.5h.01M13.5 12.5h.01M11 14h.01" strokeWidth={2.6} /></>,
  copo: <path d="M6 3h12l-1.5 17a1 1 0 0 1-1 1h-7a1 1 0 0 1-1-1zM6.5 8h11" />,
  alerta: <><path d="M12 3 2 20h20z" /><path d="M12 10v4M12 17h.01" /></>,
  livro: <><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20V3H6.5A2.5 2.5 0 0 0 4 5.5z" /><path d="M4 19.5A2.5 2.5 0 0 0 6.5 22H20v-5" /></>,
  lixeira: <path d="M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3" />,
} satisfies Record<string, ReactNode>;

export type NomeIcone = keyof typeof TRACOS;

interface IconeProps {
  nome: NomeIcone;
  tamanho?: number;
  className?: string;
}

/** Ícone decorativo em SVG; fica escondido do leitor de tela, então o texto ao lado precisa explicar. */
export default function Icone({ nome, tamanho = 24, className }: IconeProps) {
  return (
    <svg
      className={className}
      width={tamanho}
      height={tamanho}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      {TRACOS[nome]}
    </svg>
  );
}
