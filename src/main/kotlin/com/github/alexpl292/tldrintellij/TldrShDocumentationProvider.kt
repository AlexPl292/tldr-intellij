package com.github.alexpl292.tldrintellij

import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.sh.ShTypes.WORD
import com.intellij.sh.psi.ShGenericCommandDirective
import com.intellij.sh.psi.ShLiteral
import org.intellij.lang.batch.BatchTokenTypes.IDENTIFIER
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.io.FileNotFoundException
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

class TldrShDocumentationProvider : DocumentationProvider {

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (!wordWithDocumentation(element)) return null
        if (element == null) return null

        return getInfo(element.text)
    }

    private fun wordWithDocumentation(o: PsiElement?): Boolean {
        return o is LeafPsiElement && o.elementType === WORD && o.getParent() is ShLiteral && o.getParent().parent is ShGenericCommandDirective
    }
}

// Huh, it doesn't work because of some strange reason. Need to explore this later.
class TldrBatchDocumentationProvider : DocumentationProvider {

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (!wordWithDocumentation(element)) return null
        if (element == null) return null

        return getInfo(element.text)
    }

    private fun wordWithDocumentation(o: PsiElement?): Boolean {
        return o is LeafPsiElement && o.elementType === IDENTIFIER
    }
}

private val cache = ConcurrentHashMap<String, String?>()

private fun getInfo(text: String): String? = cache.computeIfAbsent(text) { downloadTldr(text) }

private fun downloadTldr(text: String): String? {
    val document = findDoc(text) ?: return null

    val flavour = CommonMarkFlavourDescriptor()

    val htmlDoc = HtmlGenerator(
        document,
        MarkdownParser(flavour).buildMarkdownTreeFromString(document),
        flavour,
        false
    ).generateHtml().removePrefix("<body>").removeSuffix("</body>")

    return buildString {
        append("<html><body>")
        append(DocumentationMarkup.DEFINITION_START)
        append("TL;DR")
        append(DocumentationMarkup.DEFINITION_END)
        append(DocumentationMarkup.SECTIONS_START)
        append(htmlDoc)
        append(DocumentationMarkup.SECTIONS_END)
        append("</body></html>")
    }
}

// TODO: 11.08.2021 Can we make it faster?
private fun findDoc(text: String): String? = pages.firstNotNullOfOrNull {
    try {
        URL("https://raw.githubusercontent.com/tldr-pages/tldr/main/pages/$it/$text.md").readText()
    } catch (e: FileNotFoundException) {
        null
    } catch (e: Throwable) {
        Logger.LOG.error(e)
        null
    }
}

private val pages = listOf(
    "common",
    "linux",
    "osx",
    "windows",
    "android",
    "sunos",
)

object Logger {
    val LOG = logger<Logger>()
}
