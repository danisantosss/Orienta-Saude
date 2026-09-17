import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import * as authApi from '../api/auth';
import { tokenStorage } from '../api/client';
import type { LoginRequest, RegisterRequest } from '../types';

interface AuthUser {
  id: number;
  email: string;
}

interface AuthContextValue {
  user: AuthUser | null;
  token: string | null;
  isAuthenticated: boolean;
  loading: boolean;
  login: (data: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function decodeUser(token: string): AuthUser | null {
  try {
    const payload = token.split('.')[1];
    const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
    if (decoded.exp && decoded.exp * 1000 < Date.now()) {
      return null;
    }
    return { id: Number(decoded.sub), email: decoded.email };
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const stored = tokenStorage.get();
    if (stored) {
      const decodedUser = decodeUser(stored);
      if (decodedUser) {
        setToken(stored);
        setUser(decodedUser);
      } else {
        tokenStorage.clear();
      }
    }
    setLoading(false);
  }, []);

  async function login(data: LoginRequest) {
    const response = await authApi.login(data);
    tokenStorage.set(response.token);
    setToken(response.token);
    setUser(decodeUser(response.token));
  }

  async function register(data: RegisterRequest) {
    await authApi.register(data);
  }

  function logout() {
    tokenStorage.clear();
    setToken(null);
    setUser(null);
  }

  const value = useMemo(
    () => ({ user, token, isAuthenticated: !!token, loading, login, register, logout }),
    [user, token, loading]
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