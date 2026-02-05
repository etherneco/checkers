# Checkers (Android)

Simple Android app with a checkers game.

## Requirements
- Android Studio (latest stable)
- JDK 11

## Run
1. Open the project in Android Studio.
2. Sync Gradle.
3. Run the `app` configuration on an emulator or device.

## Build from CLI
```powershell
./gradlew assembleDebug
```

## Structure
- `app/` � application module
- `gradle/` � Gradle wrapper and configuration

# Checkers – Rule Engine & State Logic

## Overview
This project implements the core logic of the game of checkers with a strong focus on **rule evaluation, state transitions and deterministic behaviour**.

It is intentionally not positioned as a full-featured game or UI-driven application.  
The primary goal is to model and validate game rules in a clean, predictable way.

---

## Purpose
The project was created to explore and demonstrate:
- rule-based systems
- turn-based state machines
- deterministic move validation
- handling edge cases in constrained rule environments

This makes it closer to a **game engine core** than a traditional game implementation.

---

## Key Concepts
- explicit representation of game state
- clear separation between rules and execution
- validation of legal and illegal moves
- deterministic outcomes for identical inputs

The same input state will always result in the same validated outcome.

---

## What This Project Is Not
- a UI-heavy game
- a product-oriented application
- a graphics or animation demo

The focus is on **logic and correctness**, not presentation.

---

## Why This Matters
Rule engines and state machines appear in many non-game domains, including:
- workflow systems
- approval processes
- business rules engines
- simulations
- decision trees

This project demonstrates the ability to reason about such systems in a controlled environment.

---

## Status
- Stable
- Complete for its intended scope
- Maintained as a reference implementation

---

## Notes
The project is intentionally kept simple and readable.  
No additional features are planned unless they serve the core goal of rule evaluation.
