/** Builtin Coach Heather scorer — port of server/app/engine.py */

const EMPATHY_MARKERS = [
  "understand", "i hear", "i know", "appreciate", "thank", "must be",
  "i can imagine", "that makes sense", "i'm sorry", "sorry to hear",
  "how are you", "how do you feel",
];

const ASSERTIVE_MARKERS = [
  "i want", "i need", "i'd like", "i would like", "i am asking", "i'm asking",
  "i expect", "i believe", "i think we should", "let's", "i've decided",
  "i cannot", "i can't take", "no,",
];

const HEDGES = [
  "maybe", "just", "kind of", "sort of", "i guess", "if that's okay",
  "if it's not too much", "sorry to bother", "i'm not sure", "possibly",
];

const AGGRESSIVE_MARKERS = [
  "you always", "you never", "your fault", "ridiculous", "stupid",
  "shut up", "obviously you", "you people",
];

function countMatches(text, markers) {
  return markers.reduce((n, m) => n + (text.includes(m) ? 1 : 0), 0);
}

function clamp(n) {
  return Math.max(0, Math.min(100, Math.floor(n + 0.5)));
}

export function scoreMessage(message) {
  const text = String(message).toLowerCase().trim();
  const words = text.split(/\s+/).filter(Boolean);
  const wordCount = words.length;

  const empathyHits = countMatches(text, EMPATHY_MARKERS);
  const assertiveHits = countMatches(text, ASSERTIVE_MARKERS);
  const hedgeHits = countMatches(text, HEDGES);
  const aggressiveHits = countMatches(text, AGGRESSIVE_MARKERS);
  const hasQuestion = text.includes("?");
  const hasNumbers = /\d/.test(text);

  let empathy = 35 + empathyHits * 22 + (hasQuestion ? 12 : 0);
  empathy -= aggressiveHits * 30;

  let assertiveness = 30 + assertiveHits * 22 - hedgeHits * 12;
  if (aggressiveHits > 0) assertiveness -= 10;

  let clarity = 45;
  if (wordCount >= 8 && wordCount <= 60) clarity += 25;
  else if (wordCount < 4) clarity -= 25;
  else if (wordCount > 90) clarity -= 15;
  if (hasNumbers) clarity += 10;
  clarity -= hedgeHits * 6;

  const clarityScore = clamp(clarity);
  const empathyScore = clamp(empathy);
  const assertivenessScore = clamp(assertiveness);
  const overall = clamp(
    clarityScore * 0.34 + empathyScore * 0.33 + assertivenessScore * 0.33,
  );

  const signals = {
    aggressiveHits,
    hedgeHits,
    empathyHits,
    assertiveHits,
    wordCount,
  };

  return {
    clarity: clarityScore,
    empathy: empathyScore,
    assertiveness: assertivenessScore,
    overall,
    tips: buildTips(clarityScore, empathyScore, assertivenessScore, signals),
  };
}

function buildTips(clarity, empathy, assertiveness, s) {
  const tips = [];
  if (s.aggressiveHits > 0) {
    tips.push(
      'Watch out for blaming language like "you always" - it puts people on the defensive. Describe the behavior, not the character.',
    );
  }
  if (empathy < 45) {
    tips.push(
      'Acknowledge the other person first. A line like "I know you\'ve been slammed" lowers defenses before you make your point.',
    );
  }
  if (assertiveness < 45) {
    tips.push(
      'Be more direct. Lead with a clear "I" statement such as "I\'d like..." so your ask can\'t be missed.',
    );
  }
  if (s.hedgeHits >= 2) {
    tips.push(
      'You\'re hedging a lot ("just", "maybe", "sorry"). Trimming qualifiers makes you sound more confident.',
    );
  }
  if (clarity < 45 && s.wordCount < 8) {
    tips.push(
      "Add a bit more detail - one concrete example or number makes your point land.",
    );
  }
  if (clarity < 45 && s.wordCount > 90) {
    tips.push("Tighten it up. A shorter, focused message is easier to respond to.");
  }
  if (!tips.length) {
    tips.push(
      "Nicely balanced - clear, direct, and considerate. Keep steering toward a concrete next step.",
    );
  }
  return tips;
}

export function replyTone(feedback) {
  if (feedback.overall >= 60 && feedback.empathy >= 50) return "warming";
  if (feedback.overall <= 42 || feedback.empathy <= 30) return "guarded";
  return "neutral";
}

export function moodLabel(tone) {
  return {
    warming: "opening up and more receptive",
    guarded: "defensive and cautious",
    neutral: "listening, weighing what you said",
  }[tone];
}

export function generateReply(scenario, history, userMessage, replies) {
  const feedback = scoreMessage(userMessage);
  const turnNumber = history.filter((t) => t.role === "user").length + 1;
  const tone = replyTone(feedback);
  const bank = replies[scenario.id]?.[tone];
  const reply = bank && bank.length
    ? bank[(turnNumber - 1) % bank.length]
    : "Go on - I'm listening.";
  return { reply, feedback, mood: moodLabel(tone), tone };
}
