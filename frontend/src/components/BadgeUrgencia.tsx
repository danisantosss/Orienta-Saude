import type { NivelUrgencia } from '../types';

export const ROTULOS_URGENCIA: Record<NivelUrgencia, string> = {
  LEVE: 'Leve',
  MODERADO: 'Moderado',
  URGENTE: 'Urgente',
  EMERGENCIA: 'Emergência',
};

const ICONES: Record<NivelUrgencia, string> = {
  LEVE: '●',
  MODERADO: '▲',
  URGENTE: '◆',
  EMERGENCIA: '✚',
};

/**
 * Nível de urgência com cor, ícone e rótulo próprios (Requisito 7.5): a forma do ícone
 * também muda, para quem não distingue cores.
 */
export default function BadgeUrgencia({ nivel }: { nivel: NivelUrgencia }) {
  const rotulo = ROTULOS_URGENCIA[nivel];
  return (
    <span className={`badge badge-${nivel.toLowerCase()}`} aria-label={`Nível de urgência: ${rotulo}`}>
      <span aria-hidden="true">{ICONES[nivel]}</span> {rotulo}
    </span>
  );
}
