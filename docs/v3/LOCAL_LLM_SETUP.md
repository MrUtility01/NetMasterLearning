# Local LLM Setup

NetMaster v3 exposes `LocalLlmProvider`, compatible with an Ollama-style `/api/generate` endpoint.

Example lab setup:
1. Run Ollama on a PC/VM reachable from the Android device.
2. Pull a model such as `llama3.2` (model choice is yours and depends on hardware/licensing).
3. In NetMaster → AI, enter the HTTP endpoint reachable from the phone/emulator and the model name.
4. Select **Local LLM**.

Android networking note: the sample manifest permits cleartext HTTP so a private lab endpoint such as `http://10.0.2.2:11434` can work in an emulator. For a production deployment, prefer HTTPS or isolate the endpoint on a trusted network and remove broad cleartext access.
