import { act, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import Toast, { DURACAO_TOAST_MS } from './Toast';

describe('Toast', () => {
  afterEach(() => vi.useRealTimers());

  it('mostra a mensagem e fecha sozinho depois de 3 segundos', () => {
    vi.useFakeTimers();
    const aoFechar = vi.fn();
    render(<Toast mensagem="Entrevista concluída" aoFechar={aoFechar} />);

    expect(screen.getByRole('status')).toHaveTextContent('Entrevista concluída');
    act(() => { vi.advanceTimersByTime(DURACAO_TOAST_MS - 1); });
    expect(aoFechar).not.toHaveBeenCalled();
    act(() => { vi.advanceTimersByTime(1); });
    expect(aoFechar).toHaveBeenCalledTimes(1);
    expect(DURACAO_TOAST_MS).toBe(3000);
  });
});
