package cl.duocuc.aulaviva.presentation

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for markdown preprocessing to fix rendering issues.
 */
class MarkdownPreprocessingTest {

    /**
     * Simulates the preprocessing function
     */
    private fun preprocessMarkdownText(text: String): String {
        return text
            .replace(Regex("_{2,}")) { match ->
                "\u2015".repeat(match.value.length.coerceAtMost(10))
            }
            .replace(Regex("(?<=\\s)_(?!_)(.+?)(?<!_)_(?=\\s|\\.|,|\\))")) { match ->
                "\u2015${match.groupValues[1]}\u2015"
            }
            .replace(Regex("[\u0000-\u001F&&[^\t\n\r]]"), "")
    }

    /**
     * Test: Consecutive underscores are converted to horizontal bars
     */
    @Test
    fun `consecutive underscores are converted to horizontal bars`() {
        val input = "La función ________ se utiliza para evaluar"
        val result = preprocessMarkdownText(input)
        
        // Should contain horizontal bar character instead of underscores
        assertFalse(result.contains("_____"))
        assertTrue(result.contains("―"))
    }

    /**
     * Test: Single underscores in text are preserved
     */
    @Test
    fun `single underscores in code are preserved`() {
        val input = "variable_name = my_function()"
        val result = preprocessMarkdownText(input)
        
        // Single underscores in identifiers should be kept
        assertTrue(result.contains("variable_name"))
    }

    /**
     * Test: Text without underscores is unchanged
     */
    @Test
    fun `text without underscores is unchanged`() {
        val input = "Este es un texto normal sin problemas."
        val result = preprocessMarkdownText(input)
        
        assertEquals(input, result)
    }

    /**
     * Test: Multiple blank spaces in exercises
     */
    @Test
    fun `multiple blanks in exercise are converted`() {
        val input = """
            1. La función ________ se utiliza
            2. Los operadores ________ se emplean
            3. En Kotlin, una ________ es una plantilla
        """.trimIndent()
        
        val result = preprocessMarkdownText(input)
        
        // All blanks should be converted
        assertFalse(result.contains("________"))
        assertTrue(result.contains("―"))
    }

    /**
     * Test: Control characters are removed
     */
    @Test
    fun `control characters are removed`() {
        val input = "Texto con\u0001caracteres\u0002de control"
        val result = preprocessMarkdownText(input)
        
        // Control characters should be stripped
        assertFalse(result.contains("\u0001"))
        assertFalse(result.contains("\u0002"))
    }

    /**
     * Test: Empty string returns empty
     */
    @Test
    fun `empty string returns empty`() {
        val input = ""
        val result = preprocessMarkdownText(input)
        
        assertTrue(result.isEmpty())
    }
}
