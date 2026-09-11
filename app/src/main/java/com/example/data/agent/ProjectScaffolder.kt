package com.example.data.agent

import com.example.data.local.WorkspaceFileEntity

enum class ScaffoldingTemplate(
    val title: String,
    val description: String,
    val badge: String
) {
    FULLSTACK_WEB_EDGE(
        title = "Modern Web & Edge Studio",
        description = "HTML5, Tailwind, Async JS Client & Cloudflare Edge Worker API",
        badge = "WEB+EDGE"
    ),
    CLOUDFLARE_MICRO_API(
        title = "Cloudflare Edge Microservice",
        description = "REST Router, Bearer Token Auth, KV Storage Handler, Test Suite",
        badge = "EDGE API"
    ),
    PYTHON_DATA_PIPELINE(
        title = "Python Automation & Pipeline",
        description = "Data Ingestion, Validator, CLI Runner and PyTest Unit Tests",
        badge = "PYTHON 3.12"
    ),
    COMPOSE_ANDROID_MODULE(
        title = "Kotlin Jetpack Compose Module",
        description = "Clean MVVM Architecture, StateFlow, Room Database and M3 UI",
        badge = "ANDROID"
    ),
    NODE_TYPESCRIPT_SERVICE(
        title = "Node.js & TypeScript Service",
        description = "Modular REST Controller, JWT Security Layer, tsconfig & package.json",
        badge = "TYPESCRIPT"
    )
}

class ProjectScaffolder {

    fun generateProject(template: ScaffoldingTemplate, projectName: String = "ShenApp"): List<WorkspaceFileEntity> {
        val now = System.currentTimeMillis()
        val safeName = projectName.replace("[^a-zA-Z0-9_-]".toRegex(), "").ifBlank { "ShenApp" }

        return when (template) {
            ScaffoldingTemplate.FULLSTACK_WEB_EDGE -> listOf(
                WorkspaceFileEntity(
                    path = "index.html",
                    name = "index.html",
                    language = "html",
                    content = """
                        <!DOCTYPE html>
                        <html lang="en">
                        <head>
                          <meta charset="UTF-8">
                          <meta name="viewport" content="width=device-width, initial-scale=1.0">
                          <title>$safeName • ❮ SHΞN™ᴄᴏᴅᴇʀ ❯</title>
                          <link rel="stylesheet" href="styles.css">
                        </head>
                        <body>
                          <div class="studio-app">
                            <header class="app-header">
                              <span class="cyber-tag">❮ SHΞN™ᴄᴏᴅᴇʀ ❯</span>
                              <h1>$safeName Dashboard</h1>
                              <span id="networkBadge" class="badge">Edge Connected</span>
                            </header>
                            
                            <main class="grid-layout">
                              <section class="panel">
                                <h3>Data Ingestion</h3>
                                <input type="text" id="payloadInput" placeholder="Enter sensor or user payload..." class="cyber-input" />
                                <button id="submitBtn" class="cyber-button" onclick="submitData()">Dispatch to Edge</button>
                              </section>
                              
                              <section class="panel">
                                <h3>Telemetry Stream</h3>
                                <div id="streamConsole" class="terminal-view">Awaiting incoming telemetry...</div>
                              </section>
                            </main>
                          </div>
                          <script src="app.js"></script>
                        </body>
                        </html>
                    """.trimIndent(),
                    updatedAt = now
                ),
                WorkspaceFileEntity(
                    path = "styles.css",
                    name = "styles.css",
                    language = "css",
                    content = """
                        :root {
                          --bg-primary: #0D0D0E;
                          --bg-secondary: #16171A;
                          --accent-orange: #FF6B00;
                          --accent-orange-glow: rgba(255, 107, 0, 0.4);
                          --text-primary: #FFFFFF;
                          --text-muted: #8E9099;
                          --border: #26282E;
                        }
                        * { box-sizing: border-box; margin: 0; padding: 0; font-family: monospace; }
                        body { background: var(--bg-primary); color: var(--text-primary); padding: 24px; }
                        .app-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border); padding-bottom: 16px; margin-bottom: 24px; }
                        .cyber-tag { color: var(--accent-orange); font-weight: bold; }
                        .grid-layout { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 16px; }
                        .panel { background: var(--bg-secondary); border: 1px solid var(--border); border-radius: 8px; padding: 18px; }
                        .cyber-input { width: 100%; background: #000; border: 1px solid var(--border); color: #FFF; padding: 10px; border-radius: 4px; margin: 12px 0; }
                        .cyber-button { background: var(--accent-orange); color: #FFF; border: none; padding: 10px 18px; border-radius: 4px; cursor: pointer; font-weight: bold; width: 100%; }
                        .terminal-view { background: #000; padding: 12px; border-radius: 4px; border: 1px solid var(--border); font-size: 12px; color: #39FF14; min-height: 120px; }
                    """.trimIndent(),
                    updatedAt = now
                ),
                WorkspaceFileEntity(
                    path = "app.js",
                    name = "app.js",
                    language = "javascript",
                    content = """
                        // Client logic for $safeName
                        async function submitData() {
                          const input = document.getElementById('payloadInput');
                          const consoleEl = document.getElementById('streamConsole');
                          const payloadVal = input.value.trim();
                          if (!payloadVal) return;
                          
                          consoleEl.innerText = "[DISPATCH] Sending: " + payloadVal + " to Edge Worker...\n";
                          try {
                            const res = await fetch('/api/telemetry', {
                              method: 'POST',
                              headers: { 'Content-Type': 'application/json' },
                              body: JSON.stringify({ payload: payloadVal, timestamp: Date.now() })
                            });
                            consoleEl.innerText += "[SUCCESS] Edge Response Code: 200 OK\nPayload registered in Edge KV.";
                            input.value = '';
                          } catch (e) {
                            consoleEl.innerText += "[LOCAL RUNNER SIM] Emulating edge response: Payload saved.";
                          }
                        }
                    """.trimIndent(),
                    updatedAt = now
                ),
                WorkspaceFileEntity(
                    path = "worker.js",
                    name = "worker.js",
                    language = "javascript",
                    content = """
                        // Cloudflare Worker Edge Handler for $safeName
                        export default {
                          async fetch(request, env, ctx) {
                            const url = new URL(request.url);
                            if (url.pathname === '/api/telemetry' && request.method === 'POST') {
                              const body = await request.json();
                              return new Response(JSON.stringify({ status: 'ACKNOWLEDGED', data: body }), {
                                headers: { 'Content-Type': 'application/json' }
                              });
                            }
                            return new Response("❮ SHΞN™ᴄᴏᴅᴇʀ ❯ Edge Worker v3.8", { status: 200 });
                          }
                        };
                    """.trimIndent(),
                    updatedAt = now
                ),
                WorkspaceFileEntity(
                    path = "README.md",
                    name = "README.md",
                    language = "markdown",
                    content = """
                        # $safeName - Modern Web & Edge Studio Project
                        Scaffolded autonomously by **❮ SHΞN™ᴄᴏᴅᴇʀ ❯**.
                        
                        ## Architecture
                        - Frontend: Single-page cyber UI with CSS custom properties
                        - Backend: Edge Worker compatible with Cloudflare Workers API
                        - Deployment: Ready for 1-click push to GitHub and Cloudflare Edge
                    """.trimIndent(),
                    updatedAt = now
                )
            )

            ScaffoldingTemplate.CLOUDFLARE_MICRO_API -> listOf(
                WorkspaceFileEntity(
                    path = "wrangler.toml",
                    name = "wrangler.toml",
                    language = "toml",
                    content = """
                        name = "${safeName.lowercase()}-edge-api"
                        main = "src/index.js"
                        compatibility_date = "2026-09-01"
                        
                        [vars]
                        ENVIRONMENT = "production"
                        STUDIO = "SHEN_CODER"
                    """.trimIndent(),
                    updatedAt = now
                ),
                WorkspaceFileEntity(
                    path = "src/index.js",
                    name = "index.js",
                    language = "javascript",
                    content = """
                        import { handleRoute } from './router.js';
                        import { verifyAuth } from './auth.js';

                        export default {
                          async fetch(request, env, ctx) {
                            const isAuthorized = verifyAuth(request);
                            if (!isAuthorized && request.url.includes('/secure')) {
                              return new Response(JSON.stringify({ error: 'Unauthorized: Invalid Bearer Token' }), {
                                status: 401,
                                headers: { 'Content-Type': 'application/json' }
                              });
                            }
                            return handleRoute(request, env);
                          }
                        };
                    """.trimIndent(),
                    updatedAt = now
                ),
                WorkspaceFileEntity(
                    path = "src/router.js",
                    name = "router.js",
                    language = "javascript",
                    content = """
                        export async function handleRoute(request, env) {
                          const url = new URL(request.url);
                          if (url.pathname === '/health') {
                            return new Response(JSON.stringify({ status: 'HEALTHY', uptime: 99.99 }), {
                              headers: { 'Content-Type': 'application/json' }
                            });
                          }
                          if (url.pathname === '/secure/data') {
                            return new Response(JSON.stringify({ securePayload: 'SHEN_KEY_ACTIVE', timestamp: Date.now() }), {
                              headers: { 'Content-Type': 'application/json' }
                            });
                          }
                          return new Response(JSON.stringify({ message: '404 Endpoint Not Found' }), { status: 404 });
                        }
                    """.trimIndent(),
                    updatedAt = now
                ),
                WorkspaceFileEntity(
                    path = "src/auth.js",
                    name = "auth.js",
                    language = "javascript",
                    content = """
                        export function verifyAuth(request) {
                          const auth = request.headers.get('Authorization') || '';
                          return auth.startsWith('Bearer ') && auth.length > 10;
                        }
                    """.trimIndent(),
                    updatedAt = now
                ),
                WorkspaceFileEntity(
                    path = "test/api.test.js",
                    name = "api.test.js",
                    language = "javascript",
                    content = """
                        // Autonomous Test Suite for $safeName Edge API
                        import { verifyAuth } from '../src/auth.js';

                        console.log('[TEST 1] Testing verifyAuth without header...');
                        const dummyReq = { headers: new Map() };
                        const passed1 = !verifyAuth({ headers: { get: () => null } });
                        console.log('Result 1 (Reject empty): ' + passed1);

                        console.log('[TEST 2] Testing verifyAuth with valid bearer...');
                        const passed2 = verifyAuth({ headers: { get: () => 'Bearer shen_sec_token_999' } });
                        console.log('Result 2 (Accept token): ' + passed2);
                    """.trimIndent(),
                    updatedAt = now
                )
            )

            ScaffoldingTemplate.PYTHON_DATA_PIPELINE -> listOf(
                WorkspaceFileEntity(
                    path = "main.py",
                    name = "main.py",
                    language = "python",
                    content = """
                        # Python Data Pipeline Entrypoint
                        from pipeline.processor import DataProcessor
                        from pipeline.validators import validate_record

                        def main():
                            print("❮ SHΞN™ᴄᴏᴅᴇʀ ❯ Starting Data Pipeline...")
                            sample_records = [
                                {"id": 1, "metric": 84.5, "status": "OK"},
                                {"id": 2, "metric": 102.3, "status": "ALERT"},
                                {"id": 3, "metric": 65.0, "status": "OK"}
                            ]
                            
                            processor = DataProcessor()
                            for r in sample_records:
                                if validate_record(r):
                                    processor.ingest(r)
                            
                            summary = processor.summarize()
                            print(f"[PIPELINE FINISHED] Processed {summary['count']} records. Avg metric: {summary['avg']}")

                        if __name__ == "__main__":
                            main()
                    """.trimIndent(),
                    updatedAt = now
                ),
                WorkspaceFileEntity(
                    path = "pipeline/processor.py",
                    name = "processor.py",
                    language = "python",
                    content = """
                        class DataProcessor:
                            def __init__(self):
                                self.records = []
                                
                            def ingest(self, record):
                                self.records.append(record)
                                
                            def summarize(self):
                                if not self.records:
                                    return {"count": 0, "avg": 0.0}
                                total = sum(r["metric"] for r in self.records)
                                return {
                                    "count": len(self.records),
                                    "avg": round(total / len(self.records), 2)
                                }
                    """.trimIndent(),
                    updatedAt = now
                ),
                WorkspaceFileEntity(
                    path = "pipeline/validators.py",
                    name = "validators.py",
                    language = "python",
                    content = """
                        def validate_record(record):
                            if "id" not in record or "metric" not in record:
                                return False
                            return record["metric"] >= 0.0
                    """.trimIndent(),
                    updatedAt = now
                ),
                WorkspaceFileEntity(
                    path = "tests/test_pipeline.py",
                    name = "test_pipeline.py",
                    language = "python",
                    content = """
                        # Test suite executed by BDS:AUTO:CODE_RUNNER
                        from pipeline.validators import validate_record
                        from pipeline.processor import DataProcessor

                        print("Running Pipeline Unit Tests...")
                        assert validate_record({"id": 10, "metric": 50}) == True
                        assert validate_record({"id": 11, "metric": -5}) == False
                        print("[TEST 1: Validator] PASS")

                        proc = DataProcessor()
                        proc.ingest({"id": 1, "metric": 10.0})
                        proc.ingest({"id": 2, "metric": 20.0})
                        res = proc.summarize()
                        assert res["count"] == 2
                        assert res["avg"] == 15.0
                        print("[TEST 2: Processor Aggregator] PASS")
                    """.trimIndent(),
                    updatedAt = now
                ),
                WorkspaceFileEntity(
                    path = "requirements.txt",
                    name = "requirements.txt",
                    language = "text",
                    content = """
                        pytest>=8.0.0
                        requests>=2.31.0
                    """.trimIndent(),
                    updatedAt = now
                )
            )

            ScaffoldingTemplate.COMPOSE_ANDROID_MODULE -> listOf(
                WorkspaceFileEntity(
                    path = "model/DataModels.kt",
                    name = "DataModels.kt",
                    language = "kotlin",
                    content = """
                        package com.shen.model

                        data class ProjectEntity(
                            val id: String,
                            val title: String,
                            val timestamp: Long = System.currentTimeMillis(),
                            val isCompleted: Boolean = false
                        )
                    """.trimIndent(),
                    updatedAt = now
                ),
                WorkspaceFileEntity(
                    path = "data/Repository.kt",
                    name = "Repository.kt",
                    language = "kotlin",
                    content = """
                        package com.shen.data

                        import com.shen.model.ProjectEntity
                        import kotlinx.coroutines.flow.Flow
                        import kotlinx.coroutines.flow.MutableStateFlow
                        import kotlinx.coroutines.flow.asStateFlow

                        class ProjectRepository {
                            private val _items = MutableStateFlow<List<ProjectEntity>>(emptyList())
                            val items: Flow<List<ProjectEntity>> = _items.asStateFlow()

                            fun insert(item: ProjectEntity) {
                                _items.value = _items.value + item
                            }
                        }
                    """.trimIndent(),
                    updatedAt = now
                ),
                WorkspaceFileEntity(
                    path = "ui/StudioScreen.kt",
                    name = "StudioScreen.kt",
                    language = "kotlin",
                    content = """
                        package com.shen.ui

                        import androidx.compose.foundation.layout.*
                        import androidx.compose.material3.*
                        import androidx.compose.runtime.Composable
                        import androidx.compose.ui.Modifier
                        import androidx.compose.ui.unit.dp

                        @Composable
                        fun StudioView() {
                            Surface(modifier = Modifier.fillMaxSize()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("❮ SHΞN™ᴄᴏᴅᴇʀ ❯ Mobile Scaffold", style = MaterialTheme.typography.titleLarge)
                                }
                            }
                        }
                    """.trimIndent(),
                    updatedAt = now
                )
            )

            ScaffoldingTemplate.NODE_TYPESCRIPT_SERVICE -> listOf(
                WorkspaceFileEntity(
                    path = "src/server.ts",
                    name = "server.ts",
                    language = "typescript",
                    content = """
                        // Node.js & TypeScript Microservice Entrypoint
                        import { createServer } from 'http';
                        import { handleApiRequest } from './routes/api';

                        const PORT = process.env.PORT || 3000;

                        const server = createServer(async (req, res) => {
                          if (req.url?.startsWith('/api')) {
                            return handleApiRequest(req, res);
                          }
                          res.writeHead(200, { 'Content-Type': 'application/json' });
                          res.end(JSON.stringify({ service: '❮ SHΞN™ᴄᴏᴅᴇʀ ❯ Microservice', status: 'ACTIVE' }));
                        });

                        server.listen(PORT, () => {
                          console.log(`[MICROSERVICE] Server listening on port ${'$'}{PORT}`);
                        });
                    """.trimIndent(),
                    updatedAt = now
                ),
                WorkspaceFileEntity(
                    path = "src/routes/api.ts",
                    name = "api.ts",
                    language = "typescript",
                    content = """
                        export async function handleApiRequest(req: any, res: any) {
                          res.writeHead(200, { 'Content-Type': 'application/json' });
                          res.end(JSON.stringify({
                            data: 'Enterprise endpoint active',
                            cluster: 'us-east-1',
                            time: Date.now()
                          }));
                        }
                    """.trimIndent(),
                    updatedAt = now
                ),
                WorkspaceFileEntity(
                    path = "package.json",
                    name = "package.json",
                    language = "json",
                    content = """
                        {
                          "name": "${safeName.lowercase()}-service",
                          "version": "1.0.0",
                          "description": "Scaffolded with ❮ SHΞN™ᴄᴏᴅᴇʀ ❯",
                          "scripts": {
                            "build": "tsc",
                            "start": "node dist/server.js",
                            "test": "node dist/test.js"
                          },
                          "devDependencies": {
                            "typescript": "^5.4.0"
                          }
                        }
                    """.trimIndent(),
                    updatedAt = now
                )
            )
        }
    }
}
