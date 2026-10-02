# NetMaster v3.1 — Deep Engineering Release Notes

## Product scope
NetMaster v3.1 is a deep engineering learning + network-analysis + safe-simulation foundation. It extends the original 50-level curriculum with a structured Deep Engineering Pack and deterministic troubleshooting scenarios, while adding the build pipeline required to produce APK/AAB in cloud CI.

## Content expansion
- 50 core curriculum levels remain available (650 core lessons in the current curriculum asset).
- 32 Deep Engineering Tracks (IDs 101–132).
- 160 deep engineering lessons (5 per track).
- 160 deterministic break/fix scenarios linked to troubleshooting evidence and simulator tests.
- Deep lesson schema covers: goal, mental model, technical detail, packet/state walk-through, configuration playbook, platform commands, verification, lab, failure matrix, evidence checklist, common mistakes, interview questions and mastery path.
- RAG indexes both curriculum lessons and scenario knowledge.

## Engineering / AI
- Offline hashed embeddings (384 dimensions) + lexical scoring + exact-title boost + diversity-aware retrieval.
- Persisted RAG vectors in Room.
- Local LLM adapter for Ollama and OpenAI-compatible HTTP endpoints.
- Evidence-oriented AI system prompt and safe command policy.
- Knowledge Graph persistence and Compose Canvas visualization.
- Scenario-driven Troubleshooting Coach with evidence transcript and simulator-only execution.
- Digital Twin state tied to VLAN, routing, DNS, NAT, firewall, OSPF, BGP, STP and QoS health.
- Command Simulator supports Cisco-style, MikroTik-style and common Linux/Windows diagnostic commands without touching sockets or real devices.

## Packet / configuration engineering
- Classic PCAP parser plus focused PCAPNG Enhanced Packet Block support.
- Packet records include ports, TCP flags, VLAN ID, IP version and stream ID.
- Recursive Wireshark-like display-filter subset: protocol shorthand, field comparisons, contains, boolean AND/OR/NOT and parentheses.
- Config snapshot storage + line-aware diff + fingerprints.
- Incident Workspace with lifecycle, evidence timeline and linked captures/configs.

## Build / delivery
- Gradle Wrapper files pinned to Gradle 8.9.
- GitHub Actions workflow for content validation, unit tests, debug APK, release APK/AAB artifacts and wrapper smoke test.
- Codemagic YAML for debug and release cloud builds.
- Project validation script for JSON assets and wrapper presence.

## Validation in this environment
- Manual pure-Kotlin core harness passed: `MANUAL_CORE_TESTS_OK`.
- Deep content validation: 32 tracks / 160 deep lessons / 160 scenarios / 650 core lessons.
- ZIP integrity checked with `unzip -t`.
- Full Android Gradle build is not claimed here because the working environment does not contain the Android SDK/Gradle installation required for an actual APK build.

## Boundaries
- Embeddings are dependency-free hashed feature vectors, not transformer embeddings such as E5/BGE/MiniLM.
- Packet parsing/filtering is a focused engineering subset, not a full Wireshark dissector or complete display-filter grammar.
- Local LLM support is an HTTP adapter; model weights are not bundled in the APK.
- The Digital Twin and Command Simulator are deterministic educational sandboxes, not router/switch OS emulators.
- The Autonomous Coach is simulator-driven and evidence-first; it never executes production commands or silently changes real infrastructure.
