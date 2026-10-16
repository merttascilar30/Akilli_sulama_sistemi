# Autonomous Smart Irrigation & Decision Support System

Developed as an engineering capstone project at **Elmantech**, this system addresses two critical challenges in large-scale agricultural IoT deployments: **mitigating Disk I/O bottlenecks** caused by high-frequency, sub-second time-series telemetry from edge devices (ESP32), and orchestrating soil-water balance autonomously via a **stateful Hysteresis (Deadband) mathematical model**.

---

## 1. Engineering Problem & Design Objectives

In conventional IoT automation architectures, persisting high-frequency telemetry packets from hundreds of concurrent sensor nodes directly into a relational database via synchronous (naive) transactions leads to **Connection Pool exhaustion**, **severe Disk I/O wait states**, and **database lock contention**. Furthermore, relying on primitive threshold checks (e.g., `if moisture < 30`) induces rapid valve oscillation (chatter), causing mechanical wear and water hammer effects.

To overcome these limitations, this project establishes and validates the following core engineering objectives:

1. **High-Throughput Asynchronous Ingestion:** Sustain an ingestion rate of at least **200+ messages/second (msg/s)** while maintaining end-to-end processing latency strictly below **150 ms** (both median and 95th percentile).
2. **Relational Integrity with Write Optimization:** Preserve strict ACID compliance and referential integrity across operational tables in **PostgreSQL** while absorbing write bursts through lock-free in-memory queuing (`ConcurrentLinkedQueue`), **JDBC Batch Inserts**, and **B-Tree Composite Indexing**.
3. **Chatter-Free Autonomous Actuation:** Decouple decision logic into an independent **Rule Engine** microservice that governs irrigation cycles using **Available Water Capacity (AWC)** and a **Hysteresis loop**.

---

## 2. System Architecture & Service Boundaries

The system follows the **Separation of Concerns (SoC)** principle, architected around loosely coupled tiers:

```text
+-------------------------------------------------------------------------+
|                        HARDWARE SIMULATION TIER (Python)                |
|  [Multi-Threaded ESP32 Simulator] ---> (High-Frequency HTTP POST / TCP) |
+-------------------------------------------------------------------------+
                                     |
                                     v
+-------------------------------------------------------------------------+
|                        CORE BACKEND SERVER (Spring Boot)                |
|                                                                         |
|  [REST Controller] ---> [ConcurrentLinkedQueue (In-Memory Buffer)]      |
|                                     |                                   |
|                         (Scheduled Drain Every 250 ms)                  |
|                                     |                                   |
|            +------------------------+------------------------+          |
|            | (JdbcTemplate Batch)   | (HTTP REST / JSON)     | (STOMP)  |
|            v                        v                        v          |
|     [PostgreSQL DB]        [Rule Engine Service]      [WebSocket Hub]   |
+-------------------------------------------------------------------------+
             |                        |                        |
     (B-Tree Composite)       (Python / FastAPI)        (/topic/live)
             |               [Hysteresis & State]              |
             +------------------------+------------------------+
                                      |
                                      v
+-------------------------------------------------------------------------+
|                REAL-TIME MONITORING & CONTROL DASHBOARD (UI)            |
|    [Leaflet.js Dynamic GIS Map]  |  [Chart.js Live Telemetry]           |
|    [Bidirectional Control: Manual Override & Emergency Valve Shutoff]   |
+-------------------------------------------------------------------------+
```

### Inter-Service Communication Boundary (Java Spring Boot to Python FastAPI)

A well-defined **RESTful HTTP (JSON)** boundary separates the high-concurrency **Java Spring Boot** ingestion server from the **Python FastAPI** Rule Engine microservice:

* **Rationale for Polyglot Decoupling:** While the JVM excels at multi-threaded network I/O, connection pooling, and WebSocket broadcasting, Python provides a clean, extensible environment for mathematical soil-water modeling and future evapotranspiration/ML extensions.
* **Execution Flow:** As the Spring Boot background worker drains telemetry batches, it dispatches station readings to the FastAPI Rule Engine. The engine evaluates the station's hysteresis state and returns a deterministic payload (`{"sulamaGerekli": true/false}`). Upon state transitions, the backend logs the event in `vana_loglari` (valve logs) and broadcasts real-time updates to connected clients and field simulators.

---

## 3. Storage Strategy & Database Justification

While dedicated time-series databases (`InfluxDB`, `TimescaleDB`) were evaluated, **PostgreSQL** was deliberately selected to align with Elmantech's relational infrastructure and to enforce strict **ACID transactional integrity** across interdependent entities (`istasyonlar`, `sensor_metrikleri`, and `vana_loglari`).

To prevent the relational engine from collapsing under continuous sub-second write pressure, four architectural mechanisms were implemented:

1. **Non-Blocking In-Memory Buffering (`ConcurrentLinkedQueue`):** Incoming `/api/metrics` HTTP requests never block on database I/O. Payloads are enqueued in O(1) time complexity, immediately freeing servlet threads.
2. **Scheduled Batch Persistence (`JdbcTemplate Batch Insert`):** A dedicated background worker drains the queue every 250 ms, consolidating hundreds of individual sensor readings into a single network round-trip and unified database transaction.
3. **B-Tree Composite Indexing:** Time-range queries and latest-state lookups are accelerated via a composite index on `(istasyon_id, kayit_tarihi DESC)`, eliminating costly sequential table scans during live dashboard rendering.
4. **Referential Integrity & Audit Trail Protection:** Strict Foreign Key constraints link `sensor_metrikleri` and `vana_loglari` to `istasyonlar`. Attempting an arbitrary `DELETE` operation on an active station with historical telemetry is intentionally blocked at the relational level and caught by the `@ControllerAdvice` Global Exception Handler, preventing accidental loss of historical agricultural audit logs.

---

## 4. Autonomous Rule Engine & Hysteresis Mathematical Model

Rather than relying on arbitrary static thresholds, the Rule Engine models soil-water tension calibrated for clay-loam agricultural profiles (specifically the Çukurova region) using **Available Water Capacity (AWC)**.

### Mathematical Parameters

* **Field Capacity (θ_FC):** Maximum volumetric water content retained against gravity (Default: 40%).
* **Permanent Wilting Point (θ_PWP):** Minimum moisture level below which plants cannot extract water (Default: 20%).
* **Critical Trigger Threshold (θ_Critical):** Set at 50% depletion of available water to prevent crop water stress:


*(For default parameters: θ_Critical = θ_PWP + 0.5 × (θ_FC - θ_PWP))*


### Stateful Hysteresis (Deadband) Control Loop


To prevent rapid valve actuation (oscillation/chatter) caused by minor sensor noise hovering around θ_Critical (e.g., fluctuating between `between 30.0% and 29.9%`), the microservice enforces a stateful **Hysteresis deadband**:

 ```markdown                
if (theta_current <= theta_Critical):
    Valve_State(t) = 1 (OPEN)
else if (theta_current >= theta_FC - delta_margin):
    Valve_State(t) = 0 (CLOSED)
else:
    Valve_State(t) = Valve_State(t - 1)   # Maintain previous state inside deadband
```

Once irrigation is triggered, the valve remains locked in the `OPEN` state until soil moisture recovers to the upper Field Capacity `theta_FC` band.

---

## 5. Performance Benchmark & Engineering Proof (Baseline vs. Asynchronous)

To empirically validate the architecture against the design targets  `200 msg/s` throughput at under `150 ms` latency), load tests were executed using **Locust** with **200 concurrent users** hammering the `/api/metrics` ingestion endpoint under two distinct configurations:

* **Scenario A — Naive Synchronous Architecture (Baseline):** Every incoming HTTP POST request directly invokes synchronous database persistence (`Repository.save()`) on the request thread.
* **Scenario B — Proposed Asynchronous Queue + Batch Architecture:** Incoming requests are buffered in `ConcurrentLinkedQueue` and flushed to PostgreSQL in $250 \text{ ms}$ batch intervals.

### Comparative Locust Stress Test Results (200 Concurrent Users)

| Performance Metric | Design Target | Naive Synchronous (Baseline) | Proposed Async + Batch Architecture | Engineering Gain / Impact |
| :--- | :---: | :---: | :---: | :--- |
| **Total Throughput (RPS)** | At least 200 msg/s | 274 req/s *(Collapsed)* | **1,650 - 1,730 req/s** | **Over 8x Target Capacity Sustained** |
| **Effective Write Throughput** | At least 200 msg/s | 0 msg/s *(100% Rejected)* | **1,650+ msg/s** | **Complete Elimination of I/O Bottleneck** |
| **Failure / Drop Rate** | 0% | **100%** *(274 fails/s)* | **0.0%** *(0 fails/s)* | **100% Reliability Under Heavy Load** |
| **Median Latency (p50)** | Under 150 ms | 720 ms | **95 ms** | **86.8% Latency Reduction** |
| **95th Percentile Latency (p95)** | Under 150 ms | 790 ms *(Peaked at 1,200 ms)* | **120 - 130 ms** | **Strictly Within 150 ms SLA** |

### Visual Proof of Benchmark Results

#### 1. Baseline Architecture (Naive Synchronous Direct-Write) — System Collapse
*Under 200 concurrent users (274 RPS), synchronous DB writes exhaust the connection pool immediately, resulting in a **100% failure rate (274 failures/s)** and response times spiking up to **790–1,200 ms**.*

![Naive Synchronous Baseline Benchmark]<img width="1655" height="924" alt="Ekran görüntüsü 2026-07-29 002501" src="https://github.com/user-attachments/assets/175d814f-78af-4b6a-bdb3-2219b74076e2" />


#### 2. Proposed Architecture (Async Queue + JDBC Batch Insert) — Zero Failures
*With the exact same 200 concurrent users, the proposed architecture effortlessly processes **1,650–1,730 RPS** with **0% failures**, keeping the 95th percentile latency steady at **120–130 ms** (well below the 150 ms threshold).*

![Asynchronous Batch Architecture Benchmark]<img width="1489" height="899" alt="Ekran görüntüsü 2026-07-29 003258" src="https://github.com/user-attachments/assets/9cb3de7d-ac83-4bd6-830a-ddb614019faf" />


---

## 6. Real-Time Monitoring Dashboard & GIS Integration

The operator interface eliminates HTTP polling overhead by subscribing to the backend's **STOMP over WebSocket** broadcast channel (`/topic/live-metrics`):

* **Dynamic GIS Map (`Leaflet.js`):** Field stations are plotted dynamically by coordinates with interactive status popups (`BEKLEMEDE` / `AKTİF SULAMA`) and active blue telemetry markers.
* **Critical Drought & Audio-Visual Alerting:** When soil moisture drops below the Permanent Wilting Point `(θ_PWP < 20%, e.g., 19.4%)` or tank levels deplete, the UI immediately triggers a high-priority **Red Critical Alert Banner (Toast)** in the top-right corner accompanied by an automated browser audio alarm, while the autonomous engine transitions the valve to `AÇIK (Otonom)` in the live audit table.
* **Live Time-Series Telemetry & Bidirectional Override:** Operators can monitor real-time metrics and switch any station from `Otonom` to `Manuel` mode (`Vanayı Kapat`) to dispatch an instant override signal via WebSocket.

![Real-Time Dashboard Overview]
<img width="1893" height="917" alt="Ekran görüntüsü 2026-10-16 150830" src="https://github.com/user-attachments/assets/116248e1-bde7-46d7-8e2b-1e1ce125e416" /><img width="1835" height="866" alt="Ekran görüntüsü 2026-10-16 150907" src="https://github.com/user-attachments/assets/b85cf6b8-9bc5-4a71-8429-32c75d5ecaed" />

---

## 7. Repository Directory Structure

```text
├── backend/                  # Java Spring Boot Core Ingestion & WebSocket Server
│   ├── src/main/java/        # Controller, Async Queue Worker, Repository, Entity, DTO, Config
│   ├── src/main/resources/   # application.properties & logback-spring.xml (SLF4J logging)
│   └── pom.xml               # Maven dependencies (Spring Web, Data JPA, WebSocket, PostgreSQL)
├── rule-engine/              # Python FastAPI Autonomous Decision Microservice
│   ├── main.py               # Hysteresis state machine & evaluation endpoints
│   └── test_rules.py         # PyTest boundary & edge-case unit tests
├── simulator/                # Hardware Simulation Tier
│   └── simulator.py          # Multi-threaded ESP32 telemetry generator & socket listener
├── frontend/                 # Real-Time Monitoring & Control UI
│   └── index.html            # TailwindCSS, Leaflet.js, Chart.js, SockJS/Stomp.js Dashboard
├── database/                 # Relational Schema & Indexing
│   └── schema.sql            # PostgreSQL DDL, Foreign Keys & B-Tree Composite Index definitions
├── tests/                    # Load & Stress Testing Suite
│   └── locustfile.py         # Locust high-concurrency stress test scenarios
└── docs/                     # Engineering Documentation & Benchmark Captures
    ├── Gereksinim_Analizi.md # Functional & Non-Functional Requirements (NFR) analysis
    ├── Kural_Motoru_Tasarimi.md # Soil-water tension & hysteresis mathematical modeling
   
```

---

## 8. Setup & Execution Guide

Follow the steps below in order to run the complete system locally:

### 1. Initialize PostgreSQL Database
```bash
psql -U postgres -c "CREATE DATABASE smart_irrigation;"
psql -U postgres -d smart_irrigation -f database/schema.sql
```

### 2. Start the Autonomous Rule Engine (Python FastAPI)
```bash
cd rule-engine
pip install fastapi uvicorn pydantic pytest
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

### 3. Build and Run the Core Backend Server (Spring Boot)
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

### 4. Launch the Real-Time Dashboard
Open `frontend/index.html` directly in a browser or serve it via Python:
```bash
cd frontend
python -m http.server 3000
# Navigate to http://localhost:3000
```

### 5. Run the ESP32 Simulator & Locust Load Tests
To start normal multi-threaded field simulation:
```bash
cd simulator
pip install requests locust
python simulator.py
```

To reproduce the stress test benchmarks:
```bash
cd tests
locust -f locustfile.py --host=http://localhost:8081
# Open http://localhost:8089 and spawn 200 users
```
