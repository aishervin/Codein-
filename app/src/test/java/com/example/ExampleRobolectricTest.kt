package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.agent.CustomSkillsEngine
import com.example.data.agent.ProjectRagEngine
import com.example.data.agent.ProjectScaffolder
import com.example.data.agent.ScaffoldingTemplate
import com.example.data.local.WorkspaceFileEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SHΞN™ᴄᴏᴅᴇʀ", appName)
  }

  @Test
  fun `test project scaffolder generates all templates`() {
    val scaffolder = ProjectScaffolder()
    for (tmpl in ScaffoldingTemplate.values()) {
      val files = scaffolder.generateProject(tmpl, "TestProject")
      assertTrue("Template ${tmpl.name} should generate files", files.isNotEmpty())
      for (f in files) {
        assertTrue("File path should not be empty", f.path.isNotBlank())
        assertTrue("File content should not be empty", f.content.isNotBlank())
      }
    }
  }

  @Test
  fun `test project rag engine searches workspace symbols`() {
    val rag = ProjectRagEngine()
    val mockFiles = listOf(
      WorkspaceFileEntity(
        path = "src/main.js",
        name = "main.js",
        language = "javascript",
        content = """
          function calculateScore() {
            return 42;
          }
        """.trimIndent()
      ),
      WorkspaceFileEntity(
        path = "src/auth.py",
        name = "auth.py",
        language = "python",
        content = """
          class AuthManager:
            def verify(token):
              pass
        """.trimIndent()
      )
    )

    val results = rag.searchWorkspace("calculateScore", mockFiles)
    assertTrue("Should find calculateScore", results.isNotEmpty())
    assertEquals("src/main.js", results.first().filePath)

    val contextBlock = rag.buildContextBlock(mockFiles)
    assertTrue("Context block should contain files", contextBlock.contains("calculateScore"))
  }

  @Test
  fun `test custom skills engine toggles and compiles prompt`() {
    val skills = CustomSkillsEngine()
    val initialList = skills.getSkills()
    assertTrue("Skills list should have items", initialList.isNotEmpty())

    val prompt = skills.compileSkillsPrompt()
    assertTrue("Compiled prompt should contain skills directive", prompt.contains("ACTIVE AGENT SKILLS"))

    val firstId = initialList.first().id
    skills.toggleSkill(firstId)
    val toggled = skills.getSkills().find { it.id == firstId }
    assertNotNull(toggled)
    assertEquals(false, toggled?.isEnabled)
  }
}

