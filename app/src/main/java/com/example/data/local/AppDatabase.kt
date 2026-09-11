package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        LlmEndpointEntity::class,
        WorkspaceFileEntity::class,
        ChatMessageEntity::class,
        AppConfigEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun llmDao(): LlmDao
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun chatDao(): ChatDao
    abstract fun configDao(): ConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shen_coder_studio.db"
                )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        scope.launch {
                            INSTANCE?.let { database ->
                                populateInitialData(database)
                            }
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun populateInitialData(db: AppDatabase) {
            val workspaceDao = db.workspaceDao()
            val llmDao = db.llmDao()

            // Pre-seed default workspace files for Live Preview & Editing
            val indexHtml = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>❮ SHΞN™ᴄᴏᴅᴇʀ ❯ Sandbox</title>
                  <link rel="stylesheet" href="styles.css">
                </head>
                <body>
                  <div class="cyber-container">
                    <div class="glow-banner">❮ SHΞN™ᴄᴏᴅᴇʀ ❯</div>
                    <h2>Autonomous AI Studio Sandbox</h2>
                    <p class="status">System Status: <span class="neon-online">ONLINE</span></p>
                    <div class="metrics">
                      <div class="metric-card">
                        <span class="label">ENGINE</span>
                        <span class="val">v3.8-PRO</span>
                      </div>
                      <div class="metric-card">
                        <span class="label">SANDBOX</span>
                        <span class="val">ACTIVE</span>
                      </div>
                      <div class="metric-card">
                        <span class="label">GIT SYNC</span>
                        <span class="val">READY</span>
                      </div>
                    </div>
                    <button id="actionBtn" class="cyber-btn" onclick="triggerCyberAction()">RUN DIAGNOSTIC</button>
                    <div id="outputConsole" class="terminal-box">Ready for autonomous deployment.</div>
                  </div>
                  <script src="script.js"></script>
                </body>
                </html>
            """.trimIndent()

            val stylesCss = """
                body {
                  margin: 0;
                  padding: 20px;
                  background-color: #0D0D0E;
                  color: #FFFFFF;
                  font-family: 'Courier New', monospace;
                  box-sizing: border-box;
                }
                .cyber-container {
                  border: 1px solid #26282E;
                  border-radius: 12px;
                  padding: 20px;
                  background: #16171A;
                  box-shadow: 0 0 20px rgba(255, 107, 0, 0.15);
                }
                .glow-banner {
                  color: #FF6B00;
                  font-size: 22px;
                  font-weight: bold;
                  letter-spacing: 2px;
                  text-shadow: 0 0 10px rgba(255, 107, 0, 0.6);
                  margin-bottom: 12px;
                }
                .status {
                  font-size: 14px;
                  color: #A0A0A5;
                }
                .neon-online {
                  color: #00FF88;
                  font-weight: bold;
                  text-shadow: 0 0 8px #00FF88;
                }
                .metrics {
                  display: flex;
                  gap: 12px;
                  margin: 18px 0;
                }
                .metric-card {
                  flex: 1;
                  background: #0D0D0E;
                  border: 1px solid #26282E;
                  padding: 10px;
                  border-radius: 8px;
                  text-align: center;
                }
                .metric-card .label {
                  display: block;
                  font-size: 10px;
                  color: #888;
                }
                .metric-card .val {
                  font-size: 14px;
                  color: #FF8800;
                  font-weight: bold;
                }
                .cyber-btn {
                  background: linear-gradient(135deg, #FF6B00, #FF8800);
                  color: #FFFFFF;
                  border: none;
                  padding: 12px 24px;
                  border-radius: 6px;
                  font-weight: bold;
                  cursor: pointer;
                  letter-spacing: 1px;
                  width: 100%;
                  margin-top: 10px;
                  box-shadow: 0 4px 15px rgba(255, 107, 0, 0.4);
                }
                .terminal-box {
                  margin-top: 16px;
                  background: #000000;
                  border: 1px solid #FF6B00;
                  border-radius: 6px;
                  padding: 12px;
                  color: #00FF88;
                  font-size: 12px;
                  min-height: 48px;
                }
            """.trimIndent()

            val scriptJs = """
                function triggerCyberAction() {
                  const consoleBox = document.getElementById('outputConsole');
                  consoleBox.innerHTML = "Executing real-time sandbox diagnostics...<br>";
                  let step = 0;
                  const steps = [
                    "Checking Git Data API refs... [OK]",
                    "Validating local in-memory AST... [OK]",
                    "Verifying Cloudflare Edge routes... [OK]",
                    "❮ SHΞN™ᴄᴏᴅᴇʀ ❯ Sandbox verified. All systems nominal."
                  ];
                  const interval = setInterval(() => {
                    if (step < steps.length) {
                      consoleBox.innerHTML += "> " + steps[step] + "<br>";
                      step++;
                    } else {
                      clearInterval(interval);
                    }
                  }, 400);
                }
            """.trimIndent()

            workspaceDao.upsertFile(WorkspaceFileEntity("index.html", "index.html", indexHtml, indexHtml, "html"))
            workspaceDao.upsertFile(WorkspaceFileEntity("styles.css", "styles.css", stylesCss, stylesCss, "css"))
            workspaceDao.upsertFile(WorkspaceFileEntity("script.js", "script.js", scriptJs, scriptJs, "javascript"))

            // Seed default endpoint
            llmDao.insertEndpoint(
                LlmEndpointEntity(
                    name = "OpenAI Compatible / Custom Gateway",
                    baseUrl = "https://api.openai.com/v1",
                    apiKey = "",
                    modelName = "gpt-4o",
                    systemPrompt = "You are SHΞN™ Autonomous Coding Agent. You deliver concise, production-ready, clean code without conversational filler.",
                    isDefault = true
                )
            )
        }
    }
}
