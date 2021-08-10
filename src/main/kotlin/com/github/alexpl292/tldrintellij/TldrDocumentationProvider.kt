package com.github.alexpl292.tldrintellij

import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.sh.ShTypes.WORD
import com.intellij.sh.psi.ShGenericCommandDirective
import com.intellij.sh.psi.ShLiteral
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.io.FileNotFoundException
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

class TldrDocumentationProvider : DocumentationProvider {

    private val cache = ConcurrentHashMap<String, String?>()

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (!wordWithDocumentation(element)) return null
        if (element == null) return null

        val tagText = element.text
        return cache.computeIfAbsent(tagText) { getInfo(tagText) }
    }

    private fun getInfo(text: String): String? {
        val document = try {
            URL("https://raw.githubusercontent.com/tldr-pages/tldr/main/pages/linux/$text.md").readText()
        } catch (e: FileNotFoundException) {
            return null
        } catch (e: Throwable) {
            LOG.error(e)
            return null
        }

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


    private fun wordWithDocumentation(o: PsiElement?): Boolean {
        return o is LeafPsiElement && o.elementType === WORD && o.getParent() is ShLiteral && o.getParent().parent is ShGenericCommandDirective
    }

    companion object {
        private val LOG = logger<TldrDocumentationProvider>()
    }
}
