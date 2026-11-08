import { apiClient } from './client';
import type { PerfilUsuario } from '../types';

export async function obterPerfil(): Promise<PerfilUsuario> {
  const resposta = await apiClient.get<PerfilUsuario>('/api/usuario/perfil');
  return resposta.data;
}

/** Exclui a conta e todas as triagens. A senha confirma que é mesmo o dono da conta. */
export async function excluirConta(senha: string): Promise<string> {
  const resposta = await apiClient.delete<{ mensagem: string }>('/api/usuario/conta', { data: { senha } });
  return resposta.data.mensagem;
}
