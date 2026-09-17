import { apiClient } from './client';
import type { LoginRequest, RegisterRequest, AuthResponse } from '../types';

export async function login(data: LoginRequest): Promise<AuthResponse> {
  const response = await apiClient.post<AuthResponse>('/api/auth/login', data);
  return response.data;
}

export async function register(data: RegisterRequest): Promise<void> {
  await apiClient.post('/api/auth/registro', data);
}