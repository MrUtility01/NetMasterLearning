# NetMaster V9 — Expert Deep + AI Search Architecture

## Content
All 770 core lessons are upgraded with a domain-aware expert dossier: why it matters, prerequisites, mental model, internals, state/packet walkthrough, configuration, verification, failure analysis, lab, incident, production design, expert questions and mastery contract.

## Search
The web catalog uses normalized lexical search with advanced `type:`, `tag:` and `level:` filters and indexes deep fields instead of only titles. The Android codebase already contains hashed embeddings, RAG, global search and unified catalog search; V9 treats these as one retrieval stack.

## AI
AI modes are grounded in retrieved lesson context. Local LLM adapters support Ollama and OpenAI-compatible endpoints. Real-device actions remain human-approved; simulator actions can be automated.

## Retrieval pipeline
1. Normalize Persian/English technical query.
2. Parse filters and aliases.
3. Lexical retrieval over title, internals, packet/state, config, failures and labs.
4. Vector retrieval over RAG chunks.
5. Metadata/domain filtering.
6. MMR diversity.
7. Grounded context assembly.
8. Answer with evidence and source IDs.

## Engineering principle
Symptom != root cause. A lesson is mastered only when the learner can explain, trace, configure, verify, break/fix and design it.
