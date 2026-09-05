import type { ChatTurn, Feedback, ReplyResult, Scenario } from "./types.js";

const EMPATHY_MARKERS = [
  "understand",
  "i hear",
  "i know",
  "appreciate",
  "thank",
  "must be",
  "i can imagine",
  "that makes sense",
  "i'm sorry",
  "sorry to hear",
  "how are you",
  "how do you feel",
];

const ASSERTIVE_MARKERS = [
  "i want",
  "i need",
  "i'd like",
  "i would like",
  "i am asking",
  "i'm asking",
  "i expect",
  "i believe",
  "i think we should",
  "let's",
  "i've decided",
  "i cannot",
  "i can't take",
  "no,",
];

const HEDGES = [
  "maybe",
  "just",
  "kind of",
  "sort of",
  "i guess",
  "if that's okay",
  "if it's not too much",
  "sorry to bother",
  "i'm not sure",
  "possibly",
];

const AGGRESSIVE_MARKERS = [
  "you always",
  "you never",
  "your fault",
  "ridiculous",
  "stupid",
  "shut up",
  "obviously you",
  "you people",
];

function countMatches(text: string, markers: string[]): number {
  return markers.reduce((n, m) => (text.includes(m) ? n + 1 : n), 0);
}

function clamp(n: number): number {
  return Math.max(0, Math.min(100, Math.round(n)));
}

/** Analyze the latest user message and produce coaching feedback. */
export function scoreMessage(message: string): Feedback {
  const text = message.toLowerCase().trim();
  const words = text.split(/\s+/).filter(Boolean);
  const wordCount = words.length;

  const empathyHits = countMatches(text, EMPATHY_MARKERS);
  const assertiveHits = countMatches(text, ASSERTIVE_MARKERS);
  const hedgeHits = countMatches(text, HEDGES);
  const aggressiveHits = countMatches(text, AGGRESSIVE_MARKERS);
  const hasQuestion = text.includes("?");
  const hasNumbers = /\d/.test(text);

  // Empathy: acknowledging the other person and asking about them.
  let empathy = 35 + empathyHits * 22 + (hasQuestion ? 12 : 0);
  empathy -= aggressiveHits * 30;

  // Assertiveness: clear "I" statements and direct asks, minus hedging.
  let assertiveness = 30 + assertiveHits * 22 - hedgeHits * 12;
  if (aggressiveHits > 0) assertiveness -= 10; // aggression is not assertiveness

  // Clarity: reasonable length, concreteness, not rambling.
  let clarity = 45;
  if (wordCount >= 8 && wordCount <= 60) clarity += 25;
  else if (wordCount < 4) clarity -= 25;
  else if (wordCount > 90) clarity -= 15;
  if (hasNumbers) clarity += 10;
  clarity -= hedgeHits * 6;

  const feedback: Feedback = {
    clarity: clamp(clarity),
    empathy: clamp(empathy),
    assertiveness: clamp(assertiveness),
    overall: 0,
    tips: [],
  };
  feedback.overall = clamp(
    feedback.clarity * 0.34 + feedback.empathy * 0.33 + feedback.assertiveness * 0.33,
  );

  feedback.tips = buildTips(feedback, {
    aggressiveHits,
    hedgeHits,
    empathyHits,
    assertiveHits,
    wordCount,
  });
  return feedback;
}

interface Signals {
  aggressiveHits: number;
  hedgeHits: number;
  empathyHits: number;
  assertiveHits: number;
  wordCount: number;
}

function buildTips(f: Feedback, s: Signals): string[] {
  const tips: string[] = [];
  if (s.aggressiveHits > 0) {
    tips.push(
      "Watch out for blaming language like \"you always\" — it puts people on the defensive. Describe the behavior, not the character.",
    );
  }
  if (f.empathy < 45) {
    tips.push(
      "Acknowledge the other person first. A line like \"I know you've been slammed\" lowers defenses before you make your point.",
    );
  }
  if (f.assertiveness < 45) {
    tips.push(
      "Be more direct. Lead with a clear \"I\" statement such as \"I'd like…\" so your ask can't be missed.",
    );
  }
  if (s.hedgeHits >= 2) {
    tips.push(
      "You're hedging a lot (\"just\", \"maybe\", \"sorry\"). Trimming qualifiers makes you sound more confident.",
    );
  }
  if (f.clarity < 45 && s.wordCount < 8) {
    tips.push("Add a bit more detail — one concrete example or number makes your point land.");
  }
  if (f.clarity < 45 && s.wordCount > 90) {
    tips.push("Tighten it up. A shorter, focused message is easier to respond to.");
  }
  if (tips.length === 0) {
    tips.push("Nicely balanced — clear, direct, and considerate. Keep steering toward a concrete next step.");
  }
  return tips;
}

/**
 * Produce the counterpart's reply. The counterpart warms up when the user is
 * both empathetic and assertive, and gets guarded when the user is aggressive
 * or wishy-washy. Replies are scenario-specific so the practice feels real.
 */
export function generateReply(
  scenario: Scenario,
  history: ChatTurn[],
  userMessage: string,
): ReplyResult {
  const feedback = scoreMessage(userMessage);
  const turnNumber = history.filter((t) => t.role === "user").length + 1;
  const tone = replyTone(feedback);
  const reply = replyText(scenario, tone, turnNumber);
  return { reply, feedback, mood: moodLabel(tone) };
}

type Tone = "warming" | "neutral" | "guarded";

function replyTone(f: Feedback): Tone {
  if (f.overall >= 60 && f.empathy >= 50) return "warming";
  if (f.overall <= 42 || f.empathy <= 30) return "guarded";
  return "neutral";
}

function moodLabel(tone: Tone): string {
  switch (tone) {
    case "warming":
      return "opening up and more receptive";
    case "guarded":
      return "defensive and cautious";
    default:
      return "listening, weighing what you said";
  }
}

const REPLIES: Record<string, Record<Tone, string[]>> = {
  "ask-for-raise": {
    warming: [
      "Okay, I appreciate you laying that out so clearly. Those results are real. Let me look at what's possible in the budget — walk me through the numbers you have in mind.",
      "That's a fair case, honestly. I can't promise the full amount today, but I want to advocate for you. Can you send me a short summary I can take to my director?",
    ],
    neutral: [
      "I hear you. Money's tight this cycle, though. What specifically are you basing the number on?",
      "I get that you want more. Help me understand the impact — what did you ship that moved the needle?",
    ],
    guarded: [
      "Look, everyone thinks they deserve a raise. I need more than 'I work hard' to take this upstairs.",
      "That's a lot to drop on me. Budgets are frozen. What exactly are you expecting me to do here?",
    ],
  },
  "give-feedback": {
    warming: [
      "Thanks for saying it that way — that's fair. You're right that the deadlines slipped. I've been drowning, but that's on me to flag earlier. What would help?",
      "I appreciate you being honest and not just piling on. Yeah, the launch date thing hurt the team. Let's figure out a way to catch this sooner.",
    ],
    neutral: [
      "Okay… I didn't realize it was landing that hard on everyone. What deadlines are you talking about specifically?",
      "That's tough to hear. I've had a lot going on. Can you give me an example so I understand?",
    ],
    guarded: [
      "Wow. I've been putting in a ton of hours, and this is what I get? It's not all on me, you know.",
      "That feels really unfair. Everyone's behind, not just me. Why am I the one getting this talk?",
    ],
  },
  "decline-request": {
    warming: [
      "Ah, okay — I hear you that you're at capacity. I don't want to burn you out. When could you realistically pick it up?",
      "That's fair, and thanks for being straight with me. Who else do you think could take the dashboard, or should we push it a sprint?",
    ],
    neutral: [
      "Hmm, but this is pretty important to leadership. Are you sure you can't squeeze it in?",
      "I understand you're busy, but everyone's busy. Isn't there any way to make it work?",
    ],
    guarded: [
      "I really need this done, though. Can't you just find the time? It won't take long.",
      "That's disappointing. I was counting on you. So you're just saying no?",
    ],
  },
};

function replyText(scenario: Scenario, tone: Tone, turn: number): string {
  const bank = REPLIES[scenario.id]?.[tone];
  if (!bank || bank.length === 0) {
    return "Go on — I'm listening.";
  }
  return bank[(turn - 1) % bank.length];
}
