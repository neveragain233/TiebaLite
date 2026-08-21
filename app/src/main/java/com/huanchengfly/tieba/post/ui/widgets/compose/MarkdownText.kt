package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 轻量 Markdown 渲染组件, 覆盖更新日志等场景常用的语法:
 * 标题、无序/有序列表(含嵌套)、引用、分隔线、围栏代码块、
 * 以及行内的加粗/斜体/行内代码/链接。
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val blocks = remember(markdown) { MarkdownParser.parse(markdown) }
    Column(modifier = modifier) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownParser.Block.Heading -> {
                    Spacer(Modifier.height(if (block.level <= 2) 10.dp else 6.dp))
                    Text(
                        text = inlineAnnotated(block.text, textStyle),
                        style = when (block.level) {
                            1 -> MaterialTheme.typography.headlineSmall
                            2 -> MaterialTheme.typography.titleLarge
                            3 -> MaterialTheme.typography.titleMedium
                            else -> MaterialTheme.typography.titleSmall
                        },
                    )
                    Spacer(Modifier.height(if (block.level <= 2) 6.dp else 2.dp))
                }

                is MarkdownParser.Block.Paragraph -> {
                    Text(text = inlineAnnotated(block.text, textStyle), style = textStyle)
                    Spacer(Modifier.height(8.dp))
                }

                is MarkdownParser.Block.ListBlock -> {
                    block.items.forEachIndexed { index, item ->
                        val bullet = if (block.ordered) {
                            "${item.number}. "
                        } else {
                            when (item.level % 3) {
                                0 -> "• "
                                1 -> "◦ "
                                else -> "▪ "
                            }
                        }
                        Row(
                            modifier = Modifier.padding(start = (item.level * 14).dp),
                        ) {
                            Text(
                                text = bullet,
                                style = textStyle,
                                modifier = Modifier.padding(top = 1.dp),
                            )
                            Text(
                                text = inlineAnnotated(item.text, textStyle),
                                style = textStyle,
                            )
                        }
                        if (index < block.items.lastIndex) {
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                is MarkdownParser.Block.Quote -> {
                    val color = MaterialTheme.colorScheme.outline
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .drawVerticalBar(color),
                    ) {
                        Text(
                            text = inlineAnnotated(block.text, textStyle),
                            style = textStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                is MarkdownParser.Block.CodeBlock -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = block.code,
                            style = textStyle.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = (textStyle.fontSize.value * 0.9f).sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                MarkdownParser.Block.Rule -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Modifier.drawVerticalBar(color: Color): Modifier =
    this.then(
        Modifier.drawBehind {
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(0f, size.height),
                strokeWidth = 3.dp.toPx(),
            )
        }
    )

@Composable
private fun inlineAnnotated(text: String, textStyle: TextStyle): AnnotatedString {
    val uriHandler = LocalUriHandler.current
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant
    val codeColor = MaterialTheme.colorScheme.onSurfaceVariant
    return buildAnnotatedString {
        fun appendSegment(segment: InlineSegment) {
            when (segment) {
                is InlineSegment.Text -> append(segment.text)
                is InlineSegment.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    segment.content.forEach { appendSegment(it) }
                }
                is InlineSegment.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    segment.content.forEach { appendSegment(it) }
                }
                is InlineSegment.Code -> withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBackground,
                        color = codeColor,
                    )
                ) {
                    append(segment.content)
                }
                is InlineSegment.Link -> {
                    val start = length
                    withStyle(
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                        )
                    ) {
                        segment.content.forEach { appendSegment(it) }
                    }
                    addLink(
                        LinkAnnotation.Clickable(
                            tag = segment.url,
                        ) { uriHandler.openUri(segment.url) },
                        start,
                        length,
                    )
                }
            }
        }
        InlineParser(text = text, emit = ::appendSegment).run()
    }
}

private sealed interface InlineSegment {
    data class Text(val text: String) : InlineSegment
    data class Bold(val content: List<InlineSegment>) : InlineSegment
    data class Italic(val content: List<InlineSegment>) : InlineSegment
    data class Code(val content: String) : InlineSegment
    data class Link(val url: String, val content: List<InlineSegment>) : InlineSegment
}

/**
 * 把一段文本解析为行内片段序列, 支持 **bold**、*italic*、`code`、[text](url) 与 `\` 转义。
 * 解析结果仅在内部使用, 不对外暴露。
 */
private class InlineParser(
    private val text: String,
    private val emit: (InlineSegment) -> Unit,
) {
    private var index = 0

    fun run() {
        val plain = StringBuilder()
        fun flushPlain() {
            if (plain.isNotEmpty()) {
                emit(InlineSegment.Text(plain.toString()))
                plain.setLength(0)
            }
        }
        while (index < text.length) {
            val c = text[index]
            when {
                c == '\\' && index + 1 < text.length -> {
                    plain.append(text[index + 1])
                    index += 2
                }

                c == '`' -> {
                    val end = findClosing("`", index + 1)
                    if (end >= 0) {
                        flushPlain()
                        emit(InlineSegment.Code(text.substring(index + 1, end)))
                        index = end + 1
                    } else {
                        plain.append(c)
                        index++
                    }
                }

                c == '[' -> {
                    val link = parseLink()
                    if (link != null) {
                        flushPlain()
                        emit(link)
                    } else {
                        plain.append(c)
                        index++
                    }
                }

                c == '*' && index + 1 < text.length && text[index + 1] == '*' -> {
                    val end = findClosing("**", index + 2)
                    if (end >= 0) {
                        flushPlain()
                        val inner = parseRange(index + 2, end)
                        emit(InlineSegment.Bold(inner))
                        index = end + 2
                    } else {
                        plain.append(c)
                        index++
                    }
                }

                c == '*' || c == '_' -> {
                    val end = findClosing(c.toString(), index + 1)
                    if (end >= 0) {
                        flushPlain()
                        val inner = parseRange(index + 1, end)
                        emit(InlineSegment.Italic(inner))
                        index = end + 1
                    } else {
                        plain.append(c)
                        index++
                    }
                }

                else -> {
                    plain.append(c)
                    index++
                }
            }
        }
        flushPlain()
    }

    private fun parseLink(): InlineSegment.Link? {
        val labelEnd = findClosing("]", index + 1)
        if (labelEnd < 0 || labelEnd + 1 >= text.length || text[labelEnd + 1] != '(') return null
        val urlEnd = text.indexOf(')', labelEnd + 2)
        if (urlEnd < 0) return null
        val url = text.substring(labelEnd + 2, urlEnd).trim()
        if (url.isEmpty()) return null
        val content = parseRange(index + 1, labelEnd)
        index = urlEnd + 1
        return InlineSegment.Link(url, content)
    }

    private fun findClosing(token: String, from: Int): Int {
        var i = from
        while (i < text.length) {
            if (text.startsWith(token, i)) return i
            i++
        }
        return -1
    }

    private fun parseRange(start: Int, end: Int): List<InlineSegment> {
        val segments = mutableListOf<InlineSegment>()
        val saved = index
        index = start
        val sub = InlineParser(text.substring(start, end)) { segments.add(it) }
        sub.run()
        index = saved
        return segments
    }
}

private object MarkdownParser {

    sealed interface Block {
        data class Heading(val level: Int, val text: String) : Block
        data class Paragraph(val text: String) : Block
        data class ListItem(val level: Int, val number: Int, val text: String)
        data class ListBlock(val ordered: Boolean, val items: List<ListItem>) : Block
        data class Quote(val text: String) : Block
        data class CodeBlock(val code: String) : Block
        data object Rule : Block
    }

    fun parse(markdown: String): List<Block> {
        val lines = markdown.replace("\r\n", "\n").split('\n')
        val blocks = mutableListOf<Block>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() -> i++

                trimmed.startsWith("```") || trimmed.startsWith("~~~") -> {
                    val fence = trimmed.take(3)
                    val code = StringBuilder()
                    i++
                    while (i < lines.size && !lines[i].trimStart().startsWith(fence)) {
                        code.appendLine(lines[i])
                        i++
                    }
                    i++ // skip closing fence
                    blocks += Block.CodeBlock(code.toString().trimEnd('\n'))
                }

                isHeading(trimmed) -> {
                    val level = trimmed.takeWhile { it == '#' }.length
                    blocks += Block.Heading(level, trimmed.drop(level).trim())
                    i++
                }

                isListMarker(trimmed) -> {
                    val ordered = trimmed.first().isDigit()
                    val items = mutableListOf<Block.ListItem>()
                    while (i < lines.size) {
                        val t = lines[i].trimStart()
                        if (t.isEmpty() || !isListMarker(t)) break
                        val indent = lines[i].takeWhile { it == ' ' }.length / 2
                        val number = if (ordered) {
                            t.takeWhile { it.isDigit() }.toIntOrNull() ?: 1
                        } else {
                            0
                        }
                        val content = t.replace(Regex("^[-*+]\\s+|^\\d+[.)]\\s+"), "")
                        items += Block.ListItem(indent, number, content)
                        i++
                    }
                    blocks += Block.ListBlock(ordered, items)
                }

                trimmed.startsWith(">") -> {
                    val quote = StringBuilder()
                    while (i < lines.size && lines[i].trimStart().startsWith(">")) {
                        quote.appendLine(lines[i].trimStart().drop(1).trimStart())
                        i++
                    }
                    blocks += Block.Quote(quote.toString().trimEnd('\n'))
                }

                isHorizontalRule(trimmed) -> {
                    blocks += Block.Rule
                    i++
                }

                else -> {
                    val paragraph = StringBuilder()
                    while (i < lines.size) {
                        val t = lines[i].trim()
                        if (
                            t.isEmpty() || isHeading(t) || isListMarker(t) ||
                            t.startsWith(">") || isHorizontalRule(t) ||
                            t.startsWith("```") || t.startsWith("~~~")
                        ) {
                            break
                        }
                        if (paragraph.isNotEmpty()) paragraph.append(' ')
                        paragraph.append(t)
                        i++
                    }
                    blocks += Block.Paragraph(paragraph.toString())
                }
            }
        }
        return blocks
    }

    private fun isHeading(line: String): Boolean =
        line.startsWith("#") && line.length > line.dropWhile { it == '#' }.length &&
            line.dropWhile { it == '#' }.startsWith(" ")

    private fun isListMarker(line: String): Boolean {
        if (line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ")) return true
        val digit = line.takeWhile { it.isDigit() }
        if (digit.isEmpty() || digit.length > 4) return false
        val rest = line.drop(digit.length)
        return rest.startsWith(". ") || rest.startsWith(") ")
    }

    private fun isHorizontalRule(line: String): Boolean {
        if (line.length < 3) return false
        val chars = line.toSet()
        return chars.size == 1 && (chars.first() == '-' || chars.first() == '*' || chars.first() == '_')
    }
}
