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
  pergunta: string | null;
  completa: boolean;
  aviso: string | null;
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

export interface Pagina<T> {
  itens: T[];
  pagina: number;
  tamanho: number;
  totalItens: number;
  totalPaginas: number;
}

export interface FiltroHistorico {
  pagina?: number;
  tamanho?: number;
  inicio?: string;
  fim?: string;
  nivelUrgencia?: NivelUrgencia;
}

export interface FonteResultado {
  sintoma: string;
  fonte: string;
  url: string;
}

export interface PerguntaResposta {
  pergunta: string;
  resposta: string | null;
}

export interface ResultadoTriagemResponse {
  sessaoId: number;
  sintomasIniciais: string;
  idadePaciente: number;
  nivelUrgencia: NivelUrgencia;
  condicoes: string[];
  explicacaoEducativa: string;
  orientacoesGerais: string;
  sinaisAlerta: string[];
  quandoBuscarAtendimento: string;
  notaLimitacao: string | null;
  disclaimer: string;
  fontes: FonteResultado[];
  perguntasERespostas: PerguntaResposta[];
  concluidaEm: string | null;
}

// Usuário
export interface PerfilUsuario {
  nome: string;
  email: string;
  criadoEm: string;
}
