# NetMaster v5.0 — Unified Engineering Upgrade

## What changed
- Promoted the Master Curriculum, Labs and AI Playbooks to first-class runtime content.
- Added a unified static catalog search covering Level, Track, Deep Track, Scenario, Lab and AI Playbook.
- Extended AI grounding so prompts receive both vector RAG evidence and catalog evidence.
- Kept the existing Core, Deep Engineering, Break/Fix, PCAP, Simulator, Digital Twin, Incident, Review and Knowledge Graph engines intact.
- Updated Android version to 5.0.0 / versionCode 500.

## Search contract
The central search now merges:
1. persisted operational data: lessons, notes, incidents, configs, captures
2. static engineering catalog: tracks, deep tracks, scenarios, labs, AI playbooks

Results are deduplicated by kind + id and capped at 50.

## AI safety
- Production changes are never executed by the offline AI layer.
- Commands are guidance unless executed inside the simulator.
- Evidence is separated from confidence; confidence never replaces evidence.
- Credentials/secrets must not be placed into prompts or indexed content.
