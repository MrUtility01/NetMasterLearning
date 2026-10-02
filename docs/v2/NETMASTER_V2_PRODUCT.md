# NetMaster v2 — Product Implementation

v2 turns the v1 foundation into an engineering platform.

## Implemented in code
- Local RAG baseline: chunking + weighted lexical retrieval.
- AI grounding: retrieved curriculum context is passed to the provider.
- Knowledge Graph persistence: nodes + typed edges in Room.
- Digital Twin baseline with fault injection and evidence scoring.
- PCAP/text capture analysis engine with protocol extraction and findings.
- Config snapshots + deterministic line diff + fingerprinting.
- Incident management entities, evidence, lifecycle and resolution.
- Smart Review scheduler (SM-2-inspired deterministic interval/ease model).

## Architecture
UI -> ViewModel -> Domain engines -> Room/content -> replaceable AI provider.

AI execution remains behind `AiProvider`; production adapters can be added without coupling the UI to a vendor.

## Safety
AI may recommend commands/configuration. The product does not silently execute production commands. Lab execution can be simulated by the Digital Twin.
