# RMD Intelligent Agent — Backend

Autonomous AI agent for IRA Required Minimum Distribution (RMD) management. Built with Spring Boot + Ollama (local AI).

**Branch:** `feature/local-poc-ui` | **Port:** `8085`

---

## What This Does

- Calculates IRS-mandated RMD amount from client age and IRA balance
- Analyzes 31 assets across 10 asset classes for tax-efficient liquidation
- Selects assets with losses/underperformance first to minimize tax impact
- Shows tax savings vs naive (random) asset selection approach
- Generates reinvestment suggestions for the distributed cash
- AI chat assistant powered by Ollama (runs 100% locally, no API key needed)

---

## Prerequisites

Install these before running the project:

### 1. Java 17
```powershell
winget install Microsoft.OpenJDK.17 --accept-package-agreements --accept-source-agreements
```
Verify: `java -version`

### 2. Apache Maven
```powershell
# Download Maven
Invoke-WebRequest -Uri "https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip" -OutFile "$env:USERPROFILE\maven.zip" -UseBasicParsing

# Extract
Expand-Archive "$env:USERPROFILE\maven.zip" -DestinationPath "$env:USERPROFILE\maven"

# Add to PATH (run this every new terminal session)
$env:PATH = "$env:USERPROFILE\maven\apache-maven-3.9.6\bin;$env:PATH"
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```
Verify: `mvn -version`

### 3. Ollama (Local AI — Free, No API Key)
```powershell
# Install Ollama
winget install Ollama.Ollama --accept-package-agreements --accept-source-agreements

# Pull the Phi-3 model (3.8B — runs on CPU, no GPU required)
ollama pull phi3

# Verify
ollama list
```
Ollama runs automatically as a Windows service on port `11434`.

> **Alternative models (all free):**
> - `ollama pull llama3.2` — Meta LLaMA 3.2 3B
> - `ollama pull mistral` — Mistral 7B

---

## Installation & Run

```powershell
# 1. Clone the repo
git clone https://github.com/imzerocool99/rmd-agent.git
cd rmd-agent

# 2. Checkout the POC branch
git checkout feature/local-poc-ui

# 3. Set Java & Maven in PATH
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:USERPROFILE\maven\apache-maven-3.9.6\bin;$env:PATH"

# 4. Build
mvn clean package -DskipTests

# 5. Run
java -jar target/rmd-agent-1.0.0.jar
```

Backend starts on **http://localhost:8085**

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/agent/run` | Run full agent pipeline |
| GET | `/agent/portfolio` | Get Alpaca portfolio |
| GET | `/agent/positions` | Get current positions |
| GET | `/agent/logs` | Agent execution logs |
| POST | `/agent/chat` | AI chat assistant |
| POST | `/api/analyze` | Custom portfolio analysis |
| POST | `/agent/config/limit` | Update trade limit |

### Example: Run Agent
```bash
curl -X POST http://localhost:8085/agent/run \
  -H "Content-Type: application/json" \
  -d '{"age": 75, "balance": 500000, "clientId": "client_001", "preference": "sell"}'
```

---

## Configuration (`src/main/resources/application.properties`)

```properties
server.port=8085
ollama.url=http://localhost:11434/api/generate
ollama.model=phi3
alpaca.base.url=https://paper-api.alpaca.markets/v2
trade.max.qty=5
```

---

## Project Structure

```
src/main/java/com/rmd/
├── agent/
│   ├── MasterAgent.java          # Main orchestrator — SENSE→THINK→PLAN→ACT→REPORT
│   └── Scheduler.java            # Runs agent on schedule (every hour)
├── controller/
│   ├── AgentController.java      # /agent/* endpoints
│   ├── RmdController.java        # /api/analyze endpoint
│   └── ChatController.java       # /agent/chat AI endpoint
├── llm/
│   └── AzureLLM.java             # Ollama integration (phi3) with rule-based fallback
├── logic/
│   ├── RMDService.java           # IRS RMD calculation
│   ├── AssetRankingService.java  # Ranks assets by performance
│   ├── AssetSelectionService.java # Selects assets for liquidation (tax-optimized)
│   ├── TaxService.java           # Tax impact analysis
│   ├── BondAllocationService.java # Bond-specific allocation
│   ├── PredictionService.java    # Market prediction
│   └── ReinvestmentSuggestionService.java  # Post-RMD reinvestment products
├── mcp/
│   ├── AlpacaMCP.java            # Alpaca trade execution
│   └── AlpacaPortfolioService.java
├── memory/
│   └── MemoryService.java        # Per-client decision history
├── monitor/
│   └── MonitorService.java       # Logging & alerts
└── model/
    └── RequestModel.java
```

---

## Asset Classes Covered

| Class | Examples |
|-------|---------|
| US Equity | AAPL, MSFT, GOOGL, JPM, NVDA, TSLA |
| International Equity | EFA, EEM, VEA |
| Bond | BND, AGG, TLT |
| Corporate Bond | LQD |
| High Yield Bond | HYG |
| Municipal Bond | MUB |
| Real Estate (REIT) | VNQ, O, AMT |
| Commodity | GLD, SLV, USO |
| Broad Market ETF | VTI, SPY, QQQ |
| Sector ETF | XLE, XLV, XLF |
| Money Market | VMFXX |

---

## Tech Stack

| Component | Technology | Cost |
|-----------|-----------|------|
| Runtime | Java 17 (Microsoft OpenJDK) | Free |
| Framework | Spring Boot 3.2.5 | Free |
| Build | Apache Maven 3.9.6 | Free |
| AI / LLM | Ollama + Phi-3 (local) | Free |
| Trading Platform | Alpaca Paper Trading | Free |
| Package Manager | winget | Free |

**Total cost: $0**3rs34562

---

## Running Both Services Together

| Service | Command | Port |
|---------|---------|------|
| Ollama AI | `ollama serve` (auto-starts) | 11434 |
| Backend | `java -jar target/rmd-agent-1.0.0.jar` | 8085 |
| Frontend | See [rmd-ui repo](https://github.com/imzerocool99/rmd-ui) | 4500 |

Open the app: **http://localhost:4500**
