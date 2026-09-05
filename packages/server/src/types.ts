export interface Persona {
  name: string;
  role: string;
  /** Short description of the counterpart's disposition at the start. */
  mood: string;
}

export interface Scenario {
  id: string;
  title: string;
  summary: string;
  difficulty: "warm-up" | "moderate" | "hard";
  persona: Persona;
  /** The first line the counterpart says to open the conversation. */
  opening: string;
  /** Coaching goals the user is trying to achieve in this conversation. */
  goals: string[];
}

export interface ChatTurn {
  role: "user" | "counterpart";
  content: string;
}

export interface Feedback {
  /** 0-100 sub-scores for the latest user message. */
  clarity: number;
  empathy: number;
  assertiveness: number;
  /** 0-100 blended score. */
  overall: number;
  /** Human-readable coaching tips. */
  tips: string[];
}

export interface ReplyResult {
  reply: string;
  feedback: Feedback;
  /** How the counterpart currently feels, after this exchange. */
  mood: string;
}
