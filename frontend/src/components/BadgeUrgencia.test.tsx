import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import type { NivelUrgencia } from '../types';
import BadgeUrgencia from './BadgeUrgencia';

describe('BadgeUrgencia', () => {
  it.each<[NivelUrgencia, string]>([
    ['LEVE', 'Leve'],
    ['MODERADO', 'Moderado'],
    ['URGENTE', 'Urgente'],
    ['EMERGENCIA', 'Emergência'],
  ])('nível %s tem rótulo, cor e nome acessível próprios', (nivel, rotulo) => {
    render(<BadgeUrgencia nivel={nivel} />);

    const badge = screen.getByLabelText(`Nível de urgência: ${rotulo}`);
    expect(badge).toHaveTextContent(rotulo);
    expect(badge).toHaveClass(`badge-${nivel.toLowerCase()}`);
    expect(badge.querySelector('[aria-hidden="true"]')).not.toBeNull();
  });
});
