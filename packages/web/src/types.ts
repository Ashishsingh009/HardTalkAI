export interface Persona {
  name: string;
  role: string;
  mood: string;
}

export interface Scenario {
  id: string;
  title: string;
  summary: string;
  difficulty: "warm-up" | "moderate" | "hard";
  persona: Persona;
  opening: string;
  goals: string[];
  /** Catalog hint for a later free-tier gate. Only the polished raise is true. */
  free?: boolean;
}

export interface Feedback {
  clarity: number;
  empathy: number;
  assertiveness: number;
  overall: number;
  tips: string[];
}

export interface ChatTurn {
  role: "user" | "counterpart";
  content: string;
  feedback?: Feedback;
}

export interface ReplyResult {
  reply: string;
  feedback: Feedback;
  mood: string;
}
