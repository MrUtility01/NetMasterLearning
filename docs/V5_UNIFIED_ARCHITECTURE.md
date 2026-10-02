# NetMaster v5 — Unified Engineering Architecture

```text
                    ┌───────────────────────────┐
                    │      Android UI            │
                    │ Learning / Labs / AI / Ops │
                    └─────────────┬─────────────┘
                                  │
                    ┌─────────────▼─────────────┐
                    │   Unified Search Layer     │
                    ├───────────────────────────┤
                    │ Operational Global Search  │
                    │ Static Engineering Catalog │
                    └─────────────┬─────────────┘
                                  │
             ┌────────────────────┴────────────────────┐
             │                                         │
     ┌───────▼────────┐                       ┌────────▼────────┐
     │ Hybrid RAG     │                       │ AI Grounding   │
     │ vector+lexical │                       │ evidence first  │
     └───────┬────────┘                       └────────┬────────┘
             │                                         │
             └────────────────────┬────────────────────┘
                                  │
                    ┌─────────────▼─────────────┐
                    │   Evidence / Safety Gate   │
                    │ no production auto-change  │
                    └─────────────┬─────────────┘
                                  │
              ┌───────────────────┼──────────────────┐
              │                   │                  │
       ┌──────▼──────┐    ┌──────▼──────┐   ┌──────▼──────┐
       │ Digital Twin│    │ PCAP/Packet │   │ Incident    │
       │ + Simulator │    │ Analysis    │   │ + Evidence  │
       └─────────────┘    └─────────────┘   └─────────────┘
```

## Static catalog sources
- 77 levels / 770 lessons
- 12 master engineering phases
- 32 deep engineering tracks / 160 lessons
- 160 Break/Fix scenarios
- 48 labs
- 7 AI modes

## Dynamic operational sources
- Notes
- Incidents and evidence
- Config snapshots
- PCAP captures
- Simulator snapshots
- Review/mastery state

## AI grounding pipeline
1. Normalize user query.
2. Retrieve vector/lexical chunks.
3. Search unified static catalog.
4. Merge and deduplicate evidence.
5. Pass bounded context to the selected AI provider.
6. Require human validation before any real-device change.

## Educational pipeline
`Concept → Architecture → Packet/State → Configuration → Verification → Troubleshooting → Lab → Quiz → Mastery → Capstone`
