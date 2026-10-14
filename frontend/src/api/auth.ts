import { apiClient } from './client';
import type { LoginRequest, RegistroRequest, AutenticacaoResponse } from '../types';

export async function login(dados: LoginRequest): Promise<AutenticacaoResponse> {
  const resposta = await apiClient.post<AutenticacaoResponse>('/api/auth/login', dados);
  return resposta.data;
}

export async function registrar(dados: RegistroRequest): Promise<void> {
  await apiClient.post('/api/auth/registro', dados);
}
