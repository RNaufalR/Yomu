package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.RubySegment
import com.example.domain.engine.RubyParser
import org.junit.Assert.assertEquals
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
        assertEquals("Yomu", appName)
    }

    @Test
    fun `ruby parser extracts kanji and furigana correctly`() {
        val html = "<p>Ren memegang <ruby><rb>星空の剣</rb><rt>ほしぞらのけん</rt></ruby> miliknya.</p>"
        val segments = RubyParser.parseHtmlToRubySegments(html)
        assertTrue(segments.any { it is RubySegment.Ruby && it.baseText == "星空の剣" && it.rubyText == "ほしぞらのけん" })
    }
}
