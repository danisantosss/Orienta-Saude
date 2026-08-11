// Enums
export type UrgencyLevel = 'LEVE' | 'MODERADO' | 'URGENTE' | 'EMERGENCIA';
export type SessionStatus = 'IN_PROGRESS' | 'ANALYZING' | 'COMPLETED' | 'CANCELLED' | 'ERROR';
export type MessageRole = 'SYSTEM' | 'USER' | 'ASSISTANT';

// Auth
export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  expiresIn: number;
}

// Triage
export interface TriageStartRequest {
  symptoms: string;
  age: number;
}

export interface TriageMessageRequest {
  content: string;
}

export interface TriageMessageResponse {
  question: string;
  isComplete: boolean;
}

export interface TriageSessionResponse {
  id: number;
  initialSymptoms: string;
  patientAge: number;
  status: SessionStatus;
  startedAt: string;
  completedAt: string | null;
  urgencyLevel: UrgencyLevel | null;
}

export interface TriageResultResponse {
  id: number;
  sessionId: number;
  urgencyLevel: UrgencyLevel;
  conditions: string[];
  educationalExplanation: string;
  generalGuidance: string;
  warningSigns: string[];
  whenToSeekCare: string;
  disclaimer: string;
  limitationsNote: string | null;
}

// Error
export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

// User
export interface UserProfile {
  name: string;
  email: string;
  createdAt: string;
}
