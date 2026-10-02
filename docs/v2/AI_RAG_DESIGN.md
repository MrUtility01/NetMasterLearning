# AI/RAG Design

1. Content is chunked by concept/lab/troubleshooting/commands.
2. Retriever ranks title and body matches locally.
3. Top chunks become grounded context for the AI provider.
4. Later versions can replace the retriever with embeddings/vector search without changing the UI contract.
5. Provider abstraction supports offline deterministic, local model, and cloud model adapters.
