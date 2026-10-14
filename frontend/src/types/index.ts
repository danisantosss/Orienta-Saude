// Enums (mesmos valores dos enums do backend)
export type NivelUrgencia = 'LEVE' | 'MODERADO' | 'URGENTE' | 'EMERGENCIA';
export type StatusSessao = 'EM_PROGRESSO' | 'ANALISANDO' | 'CONCLUIDA' | 'CANCELADA' | 'ERRO';
export type PapelMensagem = 'SISTEMA' | 'USUARIO' | 'ASSISTENTE';

// Autenticação
export interface LoginRequest {
  email: string;
  senha: string;
}

export interface RegistroRequest {
  nome: string;
  email: string;
  senha: string;
}

export interface AutenticacaoResponse {
  token: string;
  expiraEm: number;
}

// Erro
export interface ErroResponse {
  timestamp: string;
  status: number;
  erro: string;
  mensagem: string;
  caminho: string;
  campos?: Record<string, string>;
}

// Triagem
export interface IniciarTriagemRequest {
  sintomas: string;
  idade: number;
}

export interface IniciarTriagemResponse {
  sessaoId: number;
  primeiraPergunta: string;
}

export interface EnviarMensagemRequest {
  conteudo: string;
}

export interface MensagemTriagemResponse {
  pergunta: string;
  completa: boolean;
}

export interface SessaoTriagemResponse {
  id: number;
  sintomasIniciais: string;
  idadePaciente: number;
  status: StatusSessao;
  iniciadaEm: string;
  finalizadaEm: string | null;
  nivelUrgencia: NivelUrgencia | null;
}

export interface ResultadoTriagemResponse {
  id: number;
  sessaoId: number;
  nivelUrgencia: NivelUrgencia;
  condicoes: string[];
  explicacaoEducativa: string;
  orientacoesGerais: string;
  sinaisAlerta: string[];
  quandoBuscarAtendimento: string;
  disclaimer: string;
  notaLimitacao: string | null;
}

// Usuário
export interface PerfilUsuario {
  nome: string;
  email: string;
  criadoEm: string;
}
