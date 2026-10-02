# NetMaster v4 Unified Architecture

## Single learning graph

Core curriculum + Field Engineering + Deep Engineering + Break/Fix all become searchable learning evidence.

### Runtime flow
User → Global Search/RAG → relevant lesson/scenario/incident/config/PCAP → AI mode → grounded answer → Lab/Simulator → evidence → mastery/review.

### Learning loop
Learn → Explain → Trace Packet/State → Configure → Verify → Break → Troubleshoot → Fix → Rollback → Quiz → Mastery → Spaced Review.

### Safety
AI can reason and propose. Production changes require human approval. Credentials/secrets are excluded from prompts. Simulator and Digital Twin are the default execution surface.

### Content packs
- `app/src/main/assets/curriculum.json`: 770 core/field lessons
- `app/src/main/assets/deep/expert_engineering.json`: 160 deep lessons
- `app/src/main/assets/deep/scenarios.json`: 160 Break/Fix scenarios
- `app/src/main/assets/master/lab_catalog.json`: 48 lab definitions
- `app/src/main/assets/master/ai_playbooks.json`: AI/RAG safety and operating modes
- `app/src/main/assets/master/netmaster_master_curriculum.json`: unified map
