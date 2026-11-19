package ai.hardtalk.source.domain.model

internal object RetryCoachingCards {
    fun completedExamplesForScenario(scenarioId: String): List<String> = when (scenarioId) {
        "ask-for-raise" -> listOf(
            "I’m asking to move up one band this cycle. I led the payments rewrite and reduced checkout errors by 20%.",
            "I understand the budget freeze. Can we agree on when we’ll revisit my raise and what you need from me?",
        )
        "give-feedback" -> listOf(
            "We missed the last two launch dates and on-call had to cover the fallout. What made it hard to flag the risk earlier?",
            "For the next release let’s flag any dependency that threatens launch in the team channel and check in on Wednesday.",
        )
        "decline-request" -> listOf(
            "I’m stretched too thin and can’t squeeze the dashboard in.",
            "I’m already stretched and I can’t squeeze the dashboard in this sprint. My time is committed to the payments cutover.",
            "I can deliver the dashboard next Tuesday, or we can move the cutover to make room on Friday. Which should we prioritize?",
        )
        else -> emptyList()
    }

    private val raise = listOf(
        RetryCoachingCard(
            id = "raise-specific-ask",
            trigger = "I was hoping we could talk about a raise. I'd like my compensation to improve.",
            guidance = "Say which band or raise you’re asking for.",
            example = "I’m asking for a move to [target band] in this compensation cycle. Can we review that before the packet locks Friday?",
        ),
        RetryCoachingCard(
            id = "raise-tenure-to-impact",
            trigger = "I've been at the same band for 18 months. I've been waiting a long time for my turn.",
            guidance = "Lead with how your responsibilities grew during those 18 months.",
            example = "Over the past 18 months my scope has grown. I took responsibility for [your part of the payments rewrite], and I’d like to review how that maps to the next band.",
        ),
        RetryCoachingCard(
            id = "raise-effort-to-outcome",
            trigger = "I put in late nights and weekends. I've been working really hard and putting in more hours.",
            guidance = "Describe what the team gained from the work.",
            example = "I helped ship the payments rewrite. My contribution was [your specific work], which led to [an outcome Dana can verify].",
        ),
        RetryCoachingCard(
            id = "raise-peer-comparison",
            trigger = "My teammate joined after me and already got a bump. That's unfair when we do the same job.",
            guidance = "Bring the comparison back to your scope and the criteria for a band change.",
            example = "I’d like to understand how my work on the payments rewrite measures against the next band. Which criteria have I met, and where do you still see gaps?",
        ),
        RetryCoachingCard(
            id = "raise-freeze-concession",
            trigger = "If the budget is frozen, never mind. We can drop it until the company has money again.",
            guidance = "Acknowledge the freeze and agree on when to revisit the decision.",
            example = "I hear that this cycle’s budget is frozen. Can we document my case now and agree on a date to revisit it?",
        ),
        RetryCoachingCard(
            id = "raise-freeze-challenge",
            trigger = "The budget freeze is an excuse. You need to find the money and make it happen.",
            guidance = "Ask which options Dana can still bring to the compensation review.",
            example = "I hear that extra budget isn’t in the packet. Before it locks, is there a way to put my band change up for review?",
        ),
        RetryCoachingCard(
            id = "raise-evidence",
            trigger = "I shipped the payments rewrite. It was a big project and I did a good job leading it.",
            guidance = "Give Dana one result they can cite in the compensation discussion.",
            example = "On the payments rewrite, I owned [your deliverable]. It changed [one result you can substantiate]. I’d like that considered for [target band].",
        ),
        RetryCoachingCard(
            id = "raise-close",
            trigger = "Thanks for hearing me out. Think about it and let me know whenever you get a chance.",
            guidance = "Close by asking who will take the next step before Friday.",
            example = "What do you need from me before Friday’s lock, and when can you tell me whether the request made it into the packet?",
        ),
    )

    private val feedback = listOf(
        RetryCoachingCard(
            id = "feedback-observed-behavior",
            trigger = "You're unreliable. You never take deadlines seriously and you always let the team down.",
            guidance = "Describe the missed commitments and their effect on the team.",
            example = "We missed the last two launch dates, and on-call had to cover the fallout. I want to discuss what would help us flag the next risk earlier.",
        ),
        RetryCoachingCard(
            id = "feedback-pattern",
            trigger = "I'm only worried about the last checkout miss. Let's talk about what happened on that one release.",
            guidance = "Name both misses so Sam can respond to the pattern you’re seeing.",
            example = "I want to look at the last two launch dates together. What warning signs did we have before each one slipped?",
        ),
        RetryCoachingCard(
            id = "feedback-impact",
            trigger = "Two dates slipped, and it made the project look bad. I was disappointed by how it went.",
            guidance = "Explain the concrete impact on the people who had to respond.",
            example = "When the launch dates slipped, on-call had to cover the fallout. Let’s work out how to give them earlier notice next time.",
        ),
        RetryCoachingCard(
            id = "feedback-dependency",
            trigger = "Payments was late, so none of this was under your control. I guess we should leave it there.",
            guidance = "Acknowledge the API delay and ask what the team could still control.",
            example = "The payments API delay was a real constraint. What could we have flagged sooner so on-call had time to prepare?",
        ),
        RetryCoachingCard(
            id = "feedback-listen",
            trigger = "I don't need to hear your reasons. We know what happened and I'm telling you what to fix.",
            guidance = "Ask what Sam saw before the date slipped.",
            example = "Sam, when did the API dependency first look like a launch risk from your side? I’d like to compare that with what the team knew at the time.",
        ),
        RetryCoachingCard(
            id = "feedback-next-step",
            trigger = "Just try harder next time. Please do better and communicate more going forward.",
            guidance = "Agree on one behavior and when to use it.",
            example = "For the next launch, can we flag a dependency in the team channel as soon as it threatens the date and name who will follow up?",
        ),
        RetryCoachingCard(
            id = "feedback-escalation",
            trigger = "Leadership sees this as a performance issue. If it happens again, I'm taking this to the skip-level.",
            guidance = "Use the conversation to agree on a change you can both follow up on.",
            example = "I’d like us to agree on how we’ll flag a launch risk and check at the next launch whether the team got enough warning.",
        ),
        RetryCoachingCard(
            id = "feedback-shared-ownership",
            trigger = "It's your responsibility to fix the whole launch process. I'll leave it to you to make sure it never happens again.",
            guidance = "Name what Sam can own and what you can do to help.",
            example = "Can you flag blockers that put your part of the launch at risk? I’ll help bring the dependency owner and on-call into the decision.",
        ),
    )

    private val boundaries = listOf(
        RetryCoachingCard(
            id = "boundaries-overcommitment",
            trigger = "I'm already stretched, but I can probably squeeze the dashboard in. I'll do both if I have to.",
            guidance = "Name what would have to move before you take on the dashboard.",
            example = "If I take the dashboard for Friday, the payments cutover will have to move. Which should take priority?",
        ),
        RetryCoachingCard(
            id = "boundaries-hedging",
            trigger = "Maybe I can help. I'm so sorry, let me see if I might have some availability.",
            guidance = "State your current commitment and say what you can offer.",
            example = "I’m committed to the payments cutover this sprint. I can take on the dashboard after that if it can wait past Friday.",
        ),
        RetryCoachingCard(
            id = "boundaries-abrupt-refusal",
            trigger = "No. That's your problem. I'm not doing the dashboard and I don't want to discuss it.",
            guidance = "Acknowledge the deadline, then explain the capacity limit.",
            example = "I see why Friday matters for the exec readout. The cutover fills my capacity this sprint, so we’ll need another owner for the dashboard to meet that date.",
        ),
        RetryCoachingCard(
            id = "boundaries-priority-choice",
            trigger = "I'm busy Friday and my calendar is full. I have a lot to do this week.",
            guidance = "Name the commitment that would be affected and ask for a priority decision.",
            example = "The team is committed to the payments cutover. Taking the dashboard on now would put that work at risk. Which commitment should we protect?",
        ),
        RetryCoachingCard(
            id = "boundaries-later-date",
            trigger = "Perhaps we can revisit the dashboard later. I'll get to it sometime down the road.",
            guidance = "Tie your offer to an actual milestone and say when you can confirm timing.",
            example = "I can pick up the dashboard after the payments cutover. I can confirm a delivery date once we’ve confirmed the cutover schedule.",
        ),
        RetryCoachingCard(
            id = "boundaries-alternate-owner",
            trigger = "Someone else can take it. Just give the dashboard to anybody who isn't me.",
            guidance = "Offer a way to hand off the work and confirm who will own it.",
            example = "If the dashboard needs to be ready Friday, can we identify another owner today? I can share the context so they can scope the work.",
        ),
        RetryCoachingCard(
            id = "boundaries-scope",
            trigger = "I'll see if I can make part of the full dashboard for Friday. I don't know what the executives will actually need.",
            guidance = "Ask what needs to be shown Friday before committing to a smaller deliverable.",
            example = "What’s the minimum the execs need to see on Friday? If an existing summary would work, we can use that and schedule the dashboard after the cutover.",
        ),
        RetryCoachingCard(
            id = "boundaries-exec-pressure",
            trigger = "Leadership already expects it, so I have no choice. I'll take the hit to keep the exec readout on track.",
            guidance = "Acknowledge leadership’s expectation and ask Priya to help make the trade-off explicit.",
            example = "I know leadership expects the dashboard Friday. Let’s tell them what moving it up means for the cutover and ask which date they want us to keep.",
        ),
    )

    fun forScenario(scenarioId: String): List<RetryCoachingCard> = when (scenarioId) {
        "ask-for-raise" -> raise
        "give-feedback" -> feedback
        "decline-request" -> boundaries
        else -> emptyList()
    }
}
