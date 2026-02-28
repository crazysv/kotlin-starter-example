🚀 Kodent
🔒 Fully Offline AI-Powered Code Analysis Platform
Kodent is a privacy-first, fully offline AI code analysis assistant built for developers who need structured security scanning, code quality insights, and AI explanations — without sending their code to the cloud.

No internet.
No external APIs.
No data leaves your device.

🧠 The Problem
Developers today rely heavily on cloud AI tools for debugging and analysis.

But that creates serious issues:

🔐 Sensitive company code must be uploaded to external servers
🌐 Internet dependency limits accessibility
💰 Enterprise security tools are expensive and complex
📦 Students and independent developers lack structured security tools
There is a gap between powerful enterprise tooling and accessible development tools.

💡 The Solution
Kodent combines:

🔒 Enterprise-grade Security Scanner
🏥 Code Health Dashboard
🤖 On-device AI Assistant
📊 Structured Code Metrics
📜 Compliance Checks
✅ Industry Standard Mapping
All running fully offline.

🔒 Security Scanner
Kodent detects:

🔴 Critical
Hardcoded secrets
SQL injection
Command injection
Path traversal
Insecure deserialization
🟠 High
SSL bypass
Cleartext HTTP
Sensitive logging
Android component misconfigurations
🟡 Medium / 🔵 Low
Weak cryptography
Unsafe random usage
Code misconfiguration
Insecure storage patterns
✅ Industry Standard Mapping
Each vulnerability is mapped to:

OWASP Top 10
OWASP Mobile Top 10
CWE identifiers
CVSS severity score
📜 Compliance Checks
Kodent checks for potential violations of:

GDPR
HIPAA
PCI DSS
SOC 2
COPPA
🏥 Code Health Dashboard
Kodent analyzes five quality dimensions:

🐛 Bug Risk
⚡ Performance
🔒 Security
📖 Readability
🧩 Complexity
It provides:

Structured issue breakdown
Expandable fix suggestions
Code metrics
Best practice detection
Deterministic scoring (0–100)
All results are generated in real time.

🤖 AI Assistant (On-Device)
Kodent includes an on-device language model capable of:

💡 Explaining code
🐛 Finding bugs
⚡ Suggesting optimizations
📊 Estimating time & space complexity
Streaming responses run fully offline using Llama.cpp.

📊 Code Metrics
Kodent calculates:

Lines of code
Function count
Nesting depth
Cyclomatic complexity
Comment ratio
val/var ratio
TODO detection
🔐 Why Kodent?
✅ Fully offline
✅ No API calls
✅ No cloud dependency
✅ Deterministic static analysis
✅ Portable developer tool
✅ Works across multiple languages
✅ Designed for privacy-sensitive environments

🏗 Architecture
Kodent uses a hybrid architecture:

🔎 Rule-based static analysis engine (deterministic)
🤖 On-device LLM engine (contextual explanation)
📱 Jetpack Compose UI
🧠 Llama.cpp backend via RunAnywhere SDK
🔮 Future Vision
Planned expansions:

Project-level multi-file analysis
Code dependency visualization
Refactoring suggestions
CI-compatible structured reports
Cross-platform interface (CLI/Desktop)
Kodent aims to become a portable, privacy-first static analysis engine for developers everywhere.

🛠 Tech Stack
Kotlin
Jetpack Compose
Llama.cpp
RunAnywhere SDK
Rule-based static analysis engine
No external APIs
🏆 Built For Hackathon
Kodent demonstrates:

Offline AI inference
Enterprise security analysis
Compliance-aware scanning
Hybrid AI + static architecture
Structured developer tooling on mobile

Built with ❤️ for privacy-first development.