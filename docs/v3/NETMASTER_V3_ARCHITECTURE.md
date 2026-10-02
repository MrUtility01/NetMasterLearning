# NetMaster v3 Architecture

## v2.1 implemented
- Offline vector RAG using an EmbeddingEngine interface and feature-hash embeddings.
- Persistent Room vector store (`rag_vectors`) containing chunk vectors.
- Knowledge Graph visualization with Compose Canvas.
- Android document picker for `.pcap`, `.pcapng`, and generic capture files.
- Minimal classic PCAP + PCAPNG Enhanced Packet Block parsing.
- Wireshark-like first-pass filters: protocol, IP source/destination/address, TCP/UDP port, frame number, info contains, `and/or/not`.
- Incident Workspace with evidence and root-cause/resolution fields.
- v3 backup schema includes incidents, captures, reviews, and RAG vectors.

## v3 implemented baseline
- `LocalLlmProvider` for an Ollama-compatible local HTTP endpoint.
- `CommandSimulator` for safe deterministic network CLI simulation.
- Digital Twin now has a simulator state and fault injection changes simulated services/routes/firewall/VLAN state.
- `TroubleshootingCoach` runs a deterministic evidence-driven test plan against the simulator.
- AI mode `AUTONOMOUS_COACH` is available, but it only proposes/executes tests inside the simulator and never touches production systems.

## Deliberate boundaries
- The current embedding implementation is a dependency-free feature-hash vectorizer, not a transformer neural embedding model. The interface allows MiniLM/E5/BGE/TFLite/ONNX later.
- Local LLM means a locally hosted Ollama-compatible service; the APK does not bundle a multi-GB language model.
- PCAP analysis is a focused parser and filter layer, not full Wireshark dissector parity.
- Digital Twin is a deterministic network simulation engine, not a complete Cisco/MikroTik/JunOS OS emulator.
