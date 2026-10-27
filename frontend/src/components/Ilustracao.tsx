import type { ReactNode } from 'react';

type Variante = 'celular' | 'pensando' | 'cuidado';

const PELE = '#f4c7a8';
const CABELO = '#1f2a44';
const BLUSA = '#4fa89b';
const BLUSA_SOMBRA = '#3f8f84';

/** Cartãozinho flutuante com um ícone simples e duas linhas de "texto". */
function CartaoFlutuante({ x, y, cor, children }: { x: number; y: number; cor: string; children: ReactNode }) {
  return (
    <g transform={`translate(${x} ${y})`}>
      <rect width="112" height="46" rx="10" fill="#fff" />
      <circle cx="22" cy="23" r="13" fill={cor} opacity="0.18" />
      <g transform="translate(13 14)" stroke={cor} strokeWidth="2.2" fill="none" strokeLinecap="round" strokeLinejoin="round">
        {children}
      </g>
      <rect x="44" y="15" width="52" height="6" rx="3" fill="#cbd5e1" />
      <rect x="44" y="27" width="36" height="6" rx="3" fill="#e2e8f0" />
    </g>
  );
}

/**
 * Ilustração da identidade visual: uma pessoa usando o celular, pensando ou
 * com um coração de cuidado. É só decoração, então fica escondida do leitor de tela.
 */
export default function Ilustracao({ variante = 'celular', className }: { variante?: Variante; className?: string }) {
  return (
    <svg className={className} viewBox="0 0 400 300" role="presentation" aria-hidden="true" focusable="false">
      <rect width="400" height="300" rx="24" fill="#e6f2f0" />
      <circle cx="320" cy="70" r="46" fill="#d3ebe6" />
      <path d="M30 300c10-60 40-90 70-96 10 30 0 70-10 96z" fill="#cfe7e1" />
      <path d="M60 300c4-40 22-62 40-68" stroke="#b7dad2" strokeWidth="3" fill="none" />

      {/* Cabelo atrás da cabeça */}
      <path d="M150 128c-8-56 26-86 58-86s64 28 56 86c-2 26 10 44 4 66-30 6-96 6-122 0-6-22 6-40 4-66z" fill={CABELO} />
      {/* Corpo */}
      <path d="M120 300c0-58 34-92 86-92s86 34 86 92z" fill={BLUSA} />
      <path d="M186 208h40l-6 18c-8 6-20 6-28 0z" fill={BLUSA_SOMBRA} />
      {/* Pescoço e cabeça */}
      <rect x="194" y="166" width="24" height="36" rx="10" fill={PELE} />
      <ellipse cx="206" cy="128" rx="40" ry="46" fill={PELE} />
      {/* Franja */}
      <path d="M166 120c6-38 30-52 50-50 22 2 34 22 32 44-18-2-40-14-50-30-6 18-18 30-32 36z" fill={CABELO} />
      {/* Rosto */}
      <circle cx="190" cy="134" r="3.4" fill={CABELO} />
      <circle cx="222" cy="134" r="3.4" fill={CABELO} />
      <circle cx="182" cy="148" r="6" fill="#f2a4a0" opacity="0.5" />
      <circle cx="230" cy="148" r="6" fill="#f2a4a0" opacity="0.5" />
      {variante === 'pensando' ? (
        <path d="M198 154c4 3 12 3 16 0" stroke="#b5655c" strokeWidth="3" fill="none" strokeLinecap="round" />
      ) : (
        <path d="M194 152c6 7 18 7 24 0" stroke="#b5655c" strokeWidth="3" fill="none" strokeLinecap="round" />
      )}

      {variante === 'celular' && (
        <>
          <path d="M262 300c2-30-4-60-30-74l-14 20c14 10 20 30 18 54z" fill={BLUSA_SOMBRA} />
          <path d="M232 226c18-8 32-26 36-46" stroke={PELE} strokeWidth="18" strokeLinecap="round" fill="none" />
          <rect x="256" y="150" width="30" height="52" rx="6" fill={CABELO} transform="rotate(12 271 176)" />
          <rect x="260" y="156" width="22" height="38" rx="3" fill="#7dd3c0" transform="rotate(12 271 176)" />
          <circle cx="262" cy="190" r="10" fill={PELE} />
          <CartaoFlutuante x={20} y={40} cor="#0f766e"><path d="M4 2v10M0 7h8" /></CartaoFlutuante>
          <CartaoFlutuante x={272} y={226} cor="#1e3a8a"><path d="M1 11V7M5 11V3M9 11V5" /></CartaoFlutuante>
        </>
      )}

      {variante === 'pensando' && (
        <>
          <path d="M236 232c-4-22 0-44 8-58" stroke={PELE} strokeWidth="18" strokeLinecap="round" fill="none" />
          <circle cx="232" cy="168" r="11" fill={PELE} />
          <g fill="#fff">
            <circle cx="280" cy="96" r="5" />
            <circle cx="296" cy="78" r="8" />
            <rect x="290" y="28" width="86" height="44" rx="22" />
          </g>
          <g fill="#4fa89b">
            <circle cx="314" cy="50" r="4.5" />
            <circle cx="333" cy="50" r="4.5" />
            <circle cx="352" cy="50" r="4.5" />
          </g>
        </>
      )}

      {variante === 'cuidado' && (
        <>
          <path d="M150 238c20 10 44 12 64 2" stroke={PELE} strokeWidth="18" strokeLinecap="round" fill="none" />
          <g transform="translate(286 40)">
            <path d="M40 74S4 52 4 28A18 18 0 0 1 40 18a18 18 0 0 1 36 10c0 24-36 46-36 46z" fill="#fff" />
            <path d="M40 30v22M29 41h22" stroke="#4fa89b" strokeWidth="6" strokeLinecap="round" />
          </g>
        </>
      )}
    </svg>
  );
}
