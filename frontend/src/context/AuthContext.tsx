import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import * as authApi from '../api/auth';
import { tokenStorage } from '../api/client';
import type { LoginRequest, PapelUsuario, RegistroRequest } from '../types';

interface UsuarioAutenticado {
  id: number;
  email: string;
  papel: PapelUsuario;
}

interface AuthContextValue {
  usuario: UsuarioAutenticado | null;
  token: string | null;
  autenticado: boolean;
  /** Só decide o que a tela mostra; quem barra o acesso de verdade é o backend (403). */
  ehAdmin: boolean;
  carregando: boolean;
  login: (dados: LoginRequest) => Promise<void>;
  registrar: (dados: RegistroRequest) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function decodificarUsuario(token: string): UsuarioAutenticado | null {
  try {
    const payload = token.split('.')[1];
    const decodificado = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
    if (decodificado.exp && decodificado.exp * 1000 < Date.now()) {
      return null;
    }
    // Tokens emitidos antes da claim "papel" existir valem como USUARIO
    const papel: PapelUsuario = decodificado.papel === 'ADMIN' ? 'ADMIN' : 'USUARIO';
    return { id: Number(decodificado.sub), email: decodificado.email, papel };
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [usuario, setUsuario] = useState<UsuarioAutenticado | null>(null);
  const [carregando, setCarregando] = useState(true);

  useEffect(() => {
    const tokenSalvo = tokenStorage.get();
    if (tokenSalvo) {
      const usuarioDecodificado = decodificarUsuario(tokenSalvo);
      if (usuarioDecodificado) {
        setToken(tokenSalvo);
        setUsuario(usuarioDecodificado);
      } else {
        tokenStorage.clear();
      }
    }
    setCarregando(false);
  }, []);

  async function login(dados: LoginRequest) {
    const resposta = await authApi.login(dados);
    tokenStorage.set(resposta.token);
    setToken(resposta.token);
    setUsuario(decodificarUsuario(resposta.token));
  }

  async function registrar(dados: RegistroRequest) {
    await authApi.registrar(dados);
  }

  function logout() {
    tokenStorage.clear();
    setToken(null);
    setUsuario(null);
  }

  const value = useMemo(
    () => ({
      usuario, token, autenticado: !!token, ehAdmin: usuario?.papel === 'ADMIN', carregando, login, registrar, logout,
    }),
    [usuario, token, carregando]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth deve ser usado dentro de um AuthProvider');
  }
  return context;
}
