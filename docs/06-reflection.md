# Reflection

## Where Copilot helped most

Copilot helped most with implementing the bookmark features and generating tests through step-by-step prompts. 
It also helped compare design options, investigate failures, and keep the assignment documents in sync while I guided the requirements and verified the application locally.

## Where I corrected or rejected Copilot’s suggestions

I clarified that the URL and title must be mandatory and that only tags are optional. When the initial planning response did not ask questions, I prompted Copilot to identify assumptions and ask clarification questions. 
During manual testing, I noticed that `http://chatgpt` was being accepted, so I corrected the validation rule to require a dotted hostname. 
I also requested consistent red validation messages and corrected the repository context after the separate `Playground` repository was mentioned.

## What I learned

I learned that clear requirements, small implementation steps, and specific acceptance criteria make Copilot’s output easier to review. 
Generated code still needs automated tests and manual verification. This assignment also showed why test databases must be isolated, applied Flyway migrations should remain unchanged, and persistence tests must account for stored timestamp precision. 
I learned to distinguish actual verification results from checks that are still pending.

## What I would do differently next time

I would verify the JDK and build environment before generating the project. I find a way to keep the JDK HOME and PATH consistent across sessions permanently for entire project, so that no need to set everytime. It will reduce the time and money.
Each time setting the environment, I should check the JDK version and ensure that the build tool (Gradle) is working correctly.
I would define URL edge cases and validation behaviour earlier, then implement one feature at a time with focused tests. 
I would ask Copilot to check its assumptions before coding and record decisions and verification results immediately after each step.

