# GamerX AI Project Handover Guide 🚀

This is a **REDACTED** version of the handover guide for the public repository. The full version with secrets is available to the project owner.

## 📌 Project Overview
GamerX AI is a state-of-the-art Android AI assistant that combines cloud-based LLM logic with on-device system execution and local edge inference capabilities.

- **Primary Goal**: A personal system-level agent that can execute shell/root commands, manage files, and interact with Android internals via natural language.
- **Tech Stack**: Kotlin, Jetpack Compose, Supabase (Backend/Auth), Room DB (Local persistence), NVIDIA NIM (Cloud LLM), Llama.cpp (Local LLM - Phase 8+).

---

## 🔑 Secrets & Credentials

> [!IMPORTANT]
> All actual secrets (API Keys, Supabase URL, Tokens) have been removed from this version to protect project integrity.

### NVIDIA NIM (Cloud LLM)
- **API Key**: `<REDACTED_OBTAIN_FROM_OWNER>`
- **Model**: `qwen/qwen2.5-coder-32b-instruct`

### Supabase (Backend & Auth)
- **Project URL**: `<REDACTED>`
- **Anon Public Key**: `<REDACTED>`
- **Auth Scheme**: `gamerx://login-callback`

### Google Sign-In
- **Server Client ID**: `428146682044-6pburs08cgdbndl8a90ok5b6etf174ms.apps.googleusercontent.com`

---

## 🏗 Project Structure
Refer to the codebase for the the detailed package structure. Key components are in `com.gamerx.ai.data` and `com.gamerx.ai.ui`.

---

## 🤖 Agentic Shell Executor (Phase 9)
GamerX AI uses an **Agentic Tool-Use Loop** in `ChatRepository.streamResponse()`.

---

## 🚀 Status & Roadmap
Phase 9 (Shell Executor) is complete. Phase 10 (True Llama.cpp) is next.
