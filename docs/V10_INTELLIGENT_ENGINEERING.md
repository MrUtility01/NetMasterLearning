# NetMaster V10 — Intelligent Engineering Layer

V10 adds an intelligence/correlation layer above the V9.1 engines.

## Core pipeline
Incident → Evidence → Retrieval → Hypotheses → Discriminating Tests → First Divergence → Root Cause → Remediation → Verification → RCA

## New offline assets
- knowledge_graph_v10.json
- adaptive_profile_v10.json
- incident_playbooks_v10.json
- packet_journeys_v10.json

## Safety
No real-device mutation is performed by the offline engine. Configuration changes require explicit human approval.

## AI
Local/offline responses are grounded in retrieved lessons and evidence. External LLMs remain adapters, not mandatory dependencies.
