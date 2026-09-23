package com.example.domain.engine

import com.example.data.model.RubySegment
import java.util.regex.Pattern

object RubyParser {

    /**
     * Parses an HTML paragraph that may contain <ruby> tags into a sequence of RubySegments.
     */
    fun parseHtmlToRubySegments(html: String): List<RubySegment> {
        val segments = mutableListOf<RubySegment>()
        // Match <ruby>(?:<rb>)?(.*?)(?:</rb>)?<rt>(.*?)</rt>(?:<rp>.*?</rp>)?</ruby>
        val rubyRegex = Pattern.compile(
            "<ruby>(?:<rb>)?([\\s\\S]*?)(?:</rb>)?<rt>([\\s\\S]*?)</rt>(?:<rp>[\\s\\S]*?</rp>)?</ruby>",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = rubyRegex.matcher(html)
        var lastEnd = 0

        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()

            // Preceding regular text
            if (start > lastEnd) {
                val preText = stripHtmlTags(html.substring(lastEnd, start))
                if (preText.isNotEmpty()) {
                    segments.add(RubySegment.Text(preText))
                }
            }

            val baseText = stripHtmlTags(matcher.group(1) ?: "")
            val rubyText = stripHtmlTags(matcher.group(2) ?: "")
            segments.add(RubySegment.Ruby(baseText, rubyText))

            lastEnd = end
        }

        if (lastEnd < html.length) {
            val postText = stripHtmlTags(html.substring(lastEnd))
            if (postText.isNotEmpty()) {
                segments.add(RubySegment.Text(postText))
            }
        }

        return if (segments.isEmpty()) {
            val clean = stripHtmlTags(html)
            if (clean.isNotEmpty()) listOf(RubySegment.Text(clean)) else emptyList()
        } else {
            segments
        }
    }

    private fun stripHtmlTags(input: String): String {
        return input
            .replace(Regex("&nbsp;"), " ")
            .replace(Regex("&lt;"), "<")
            .replace(Regex("&gt;"), ">")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("&quot;"), "\"")
            .replace(Regex("<[^>]*>"), "")
    }

    /**
     * Splits chapter HTML into list of paragraphs with ruby annotations parsed.
     */
    fun parseChapterParagraphs(rawHtml: String): List<List<RubySegment>> {
        val paragraphs = mutableListOf<List<RubySegment>>()

        // Normalize <p> tags or <div class="paragraph">
        val pPattern = Pattern.compile("<p(?:\\s+[^>]*)?>([\\s\\S]*?)</p>", Pattern.CASE_INSENSITIVE)
        val matcher = pPattern.matcher(rawHtml)
        var foundAny = false

        while (matcher.find()) {
            foundAny = true
            val inner = matcher.group(1)?.trim() ?: ""
            if (inner.isNotEmpty()) {
                val segs = parseHtmlToRubySegments(inner)
                if (segs.isNotEmpty()) {
                    paragraphs.add(segs)
                }
            }
        }

        if (!foundAny) {
            // Split by line breaks or double newlines
            val lines = rawHtml.replace(Regex("<br\\s*/?>"), "\n").split("\n")
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    val segs = parseHtmlToRubySegments(trimmed)
                    if (segs.isNotEmpty()) {
                        paragraphs.add(segs)
                    }
                }
            }
        }

        return paragraphs
    }
}
