import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import Disclaimer, { TEXTO_DISCLAIMER } from './Disclaimer';

describe('Disclaimer', () => {
  it('mostra o aviso educativo como nota para leitores de tela', () => {
    render(<Disclaimer />);

    expect(screen.getByRole('note')).toHaveTextContent(TEXTO_DISCLAIMER);
    expect(TEXTO_DISCLAIMER).toContain('não realiza diagnóstico');
    expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
  });

  it('com confirmação, avisa quando o usuário marca que leu', async () => {
    const aoConfirmar = vi.fn();
    render(<Disclaimer confirmado={false} aoConfirmar={aoConfirmar} />);

    await userEvent.click(screen.getByRole('checkbox', { name: /li e entendi/i }));

    expect(aoConfirmar).toHaveBeenCalledWith(true);
  });

  it('aceita um texto vindo do backend', () => {
    render(<Disclaimer texto="Aviso do resultado" />);

    expect(screen.getByRole('note')).toHaveTextContent('Aviso do resultado');
  });
});
