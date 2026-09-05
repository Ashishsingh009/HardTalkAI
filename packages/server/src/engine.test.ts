import { describe, it, expect } from "vitest";
import { scoreMessage, generateReply } from "./engine.js";
import { findScenario } from "./scenarios.js";

describe("scoreMessage", () => {
  it("rewards balanced, empathetic and assertive messages", () => {
    const good = scoreMessage(
      "I really appreciate how slammed you've been. I'd like us to agree on a plan so the deadlines stop slipping. Can we set a weekly check-in?",
    );
    expect(good.empathy).toBeGreaterThan(55);
    expect(good.assertiveness).toBeGreaterThan(45);
    expect(good.overall).toBeGreaterThan(60);
  });

  it("penalizes aggressive, blaming language", () => {
    const bad = scoreMessage("You always miss deadlines and it's your fault the team looks stupid.");
    expect(bad.empathy).toBeLessThan(30);
    expect(bad.tips.join(" ")).toMatch(/blaming/i);
  });

  it("flags excessive hedging", () => {
    const hedgy = scoreMessage("Sorry to bother, maybe I just kind of wanted to possibly ask something?");
    expect(hedgy.assertiveness).toBeLessThan(45);
    expect(hedgy.tips.join(" ")).toMatch(/hedg/i);
  });
});

describe("generateReply", () => {
  it("warms up the counterpart for a strong message", () => {
    const scenario = findScenario("ask-for-raise")!;
    const res = generateReply(
      scenario,
      [],
      "I appreciate you making time. I'd like to talk about a raise: I shipped 3 launches and cut latency by 40%. What's possible?",
    );
    expect(res.reply).toBeTruthy();
    expect(res.mood).toMatch(/receptive/);
    expect(res.feedback.overall).toBeGreaterThan(60);
  });

  it("makes the counterpart guarded for a weak message", () => {
    const scenario = findScenario("give-feedback")!;
    const res = generateReply(scenario, [], "you never do your work");
    expect(res.mood).toMatch(/defensive/);
  });
});
