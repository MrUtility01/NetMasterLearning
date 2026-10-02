# NetMaster v3.1 Implementation Map

| Layer | Capability | Main files | State |
|---|---|---|---|
| Content | 50 core levels + deep pack | `app/src/main/assets/curriculum.json`, `app/src/main/assets/deep/*.json` | Implemented |
| Content | 32 deep tracks / 160 lessons | `assets/deep/expert_engineering.json` | Implemented |
| Content | 160 break/fix scenarios | `assets/deep/scenarios.json`, `ScenarioEngine.kt` | Implemented |
| Retrieval | 384-d hashed embeddings | `search/EmbeddingEngine.kt` | Offline baseline |
| Retrieval | Hybrid vector + lexical + MMR-style selection | `search/RagEngine.kt` | Implemented |
| Retrieval | Room vector persistence | `data/V2Models.kt`, `data/AppDatabase.kt`, `NetMasterViewModel.kt` | Implemented |
| Knowledge | Knowledge Graph persistence | `data/V2Models.kt`, `AppDatabase.kt` | Implemented |
| Knowledge | Canvas visualization | `ui/NetMasterApp.kt` | Implemented |
| Packet | PCAP / PCAPNG | `domain/PcapParser.kt` | Focused subset |
| Packet | Wireshark-like filters | `domain/PacketFilterEngine.kt` | Focused subset |
| Packet | Text capture analysis | `domain/PacketAnalysisEngine.kt` | Implemented |
| Config | Snapshot + fingerprint + diff | `domain/ConfigDiffEngine.kt`, `data/V2Models.kt` | Implemented |
| Incident | Incident Workspace / evidence / events | `NetMasterViewModel.kt`, `NetMasterApp.kt`, `data/V2Models.kt` | Implemented |
| Review | Mastery → review queue | `domain/ReviewEngine.kt`, `NetMasterViewModel.kt` | Implemented baseline |
| AI | Offline AI modes | `ai/AiEngine.kt` | Implemented |
| AI | Ollama / OpenAI-compatible adapter | `ai/LocalLlmProvider.kt` | Implemented adapter |
| Simulation | Command Simulator | `domain/CommandSimulator.kt` | Implemented sandbox |
| Simulation | Digital Twin + fault injection | `domain/DigitalTwin.kt` | Implemented deterministic model |
| Coach | Scenario-driven autonomous troubleshooting | `domain/TroubleshootingCoach.kt` | Implemented simulator-driven |
| Persistence | Backup/restore of v2/v3 domains | `export/ExportManager.kt` | Implemented |
| Build | Gradle Wrapper 8.9 | `gradlew`, `gradlew.bat`, `gradle/wrapper/*` | Included |
| Build | GitHub Actions | `.github/workflows/android.yml` | Included |
| Build | Codemagic | `codemagic.yaml` | Included |

## Intentionally not claimed
Full transformer/vector-DB stack, complete Wireshark grammar/dissector/reassembly, multi-vendor command parity, full network-OS emulation, bundled LLM weights, autonomous production execution, and cloud/team synchronization remain later engineering phases.
