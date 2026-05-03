package org.gcb.blah.mybatis.syntax

import ai.grazie.text.range
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.patterns.XmlPatterns
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlTokenType
import com.intellij.util.ProcessingContext
import ognl.ASTProperty
import ognl.ASTVarRef
import ognl.Node
import ognl.Ognl
import org.gcb.blah.general.GeneralProjectUtils


private val SQL_TAG_LIST = listOf("select", "update", "insert", "delete")

object MybatisRawSqlParameterProvider: PsiReferenceProvider() {
    private val PARAM_REGEX = Regex("""[#$]\{([a-zA-Z_$][a-zA-Z0-9_$]*)}""")
    override fun getReferencesByElement(
        element: PsiElement,
        p1: ProcessingContext
    ): Array<out PsiReference?> {
        val targetClass = resolveParameterTypeOfDml(element) ?: return PsiReference.EMPTY_ARRAY
        val text = element.text
        val res = mutableListOf<PsiReference>()
        PARAM_REGEX.findAll(text).forEach { matchResult ->
            val innerGroup = matchResult.groups[1]
            val value = innerGroup?.value ?: return@forEach
            res.add(MybatisParamReference(element, TextRange(innerGroup.range.first, innerGroup.range.first + value.range.length), value.trimIndent(), targetClass))
        }
        return res.toTypedArray()
    }
}


object OgnlUtils {
    fun bfs(root: Node): List<Node> {
        val res = mutableListOf<Node>()
        val queue = ArrayDeque<Node>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node is ASTVarRef) {
                res.add(node)
            } else if (node is ASTProperty
                && !node.toString().startsWith("\"")
                && !node.toString().startsWith("'")) {
                res.add(node)
            }
            for(childIdx in 0 until node.jjtGetNumChildren()) {
                node.jjtGetChild(childIdx)?.let { queue.add(it) }
            }
        }
        return res
    }
}

object MybatisXmlAttrOgnlProvider: PsiReferenceProvider() {
    override fun getReferencesByElement(p0: PsiElement, p1: ProcessingContext): Array<out PsiReference?> {
        val targetClass = resolveParameterTypeOfXmlAttrOgnl(p0) ?: return PsiReference.EMPTY_ARRAY
        val expressionText = (p0 as XmlAttributeValue).value
        val res = mutableListOf<PsiReference>()
        val root = try {
            Ognl.parseExpression(expressionText) as Node
        } catch (e: Exception) {
            null
        }
        if (root == null) {
            return PsiReference.EMPTY_ARRAY
        }
        OgnlUtils.bfs(root).map { it.toString() }.forEach {
            name ->
            val regex = Regex("""\b$name\b""")
            regex.findAll(expressionText).forEach { matchResult ->
                val start = matchResult.range.first
                val end = start + name.length
                res.add(MybatisParamReference(p0, TextRange(start, end), name, targetClass))
            }
        }
        return res.toTypedArray()
    }
}

object MyBatisForeachCollectionRefProvider: PsiReferenceProvider() {
    override fun getReferencesByElement(p0: PsiElement, p1: ProcessingContext): Array<PsiReference> {
        val collectionName = (p0 as XmlAttributeValue).value
        val targetClass = resolveParameterTypeOfXmlAttr(p0) ?: return PsiReference.EMPTY_ARRAY
        val res = mutableListOf<PsiReference>()
        res.add(MybatisParamReference(p0, TextRange(0, collectionName.length), collectionName, targetClass))
        return res.toTypedArray()
    }
}

class MybatisReferenceContributor: PsiReferenceContributor() {
    override fun registerReferenceProviders(rgstr: PsiReferenceRegistrar) {
        val mybatisRawSqlPattern = PlatformPatterns.psiElement(XmlTokenType.XML_DATA_CHARACTERS)
            // 过滤层 1：必须在 XML 文件中（极速失败，保护性能）
            .inFile(XmlPatterns.xmlFile())
            // 过滤层 2：向上遍历，必须被包裹在这些特定的标签中
            .inside(
                XmlPatterns.xmlTag().withName(
                    StandardPatterns.string().oneOf("select", "insert", "update", "delete", "sql")
                )
            )
        rgstr.registerReferenceProvider(mybatisRawSqlPattern, MybatisRawSqlParameterProvider)
        val mybatisXmlAttributeOgnlPattern = XmlPatterns.xmlAttributeValue()
            .withParent(
                XmlPatterns.xmlAttribute("test").withParent(
                    XmlPatterns.xmlTag().withName(
                        PlatformPatterns.string().oneOf("if")
                    )
                )
            )
        rgstr.registerReferenceProvider(mybatisXmlAttributeOgnlPattern,
            MybatisXmlAttrOgnlProvider
        )
        val mybatisForeachCollectionRefPattern = XmlPatterns.xmlAttributeValue()
            .withParent(
                XmlPatterns.xmlAttribute("collection").withParent(
                    XmlPatterns.xmlTag().withName(
                        PlatformPatterns.string().oneOf("foreach")
                    )
                )
            )
        rgstr.registerReferenceProvider(mybatisForeachCollectionRefPattern, MyBatisForeachCollectionRefProvider)
    }
}

private fun resolveParameterTypeOfDml(element: PsiElement): PsiClass? {
    return resolveParameterTypeOfMybatisText(element)
}

private fun resolveParameterTypeOfMybatisText(element: PsiElement): PsiClass? {
    val tag = PsiTreeUtil.findFirstParent(element) {
        it is XmlTag && it.name in SQL_TAG_LIST
    } as? XmlTag ?: return null
    val typeStr = tag.getAttributeValue("parameterType") ?: return null
    return GeneralProjectUtils.findClazzExistInProject(element.project, typeStr)
}

private fun resolveParameterTypeOfXmlAttr(element: PsiElement): PsiClass? {
    return resolveParameterTypeOfMybatisText(element)
}

private fun resolveParameterTypeOfXmlAttrOgnl(element: PsiElement): PsiClass? {
    return resolveParameterTypeOfXmlAttr(element)
}