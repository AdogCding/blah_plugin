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
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlTokenType
import com.intellij.util.ProcessingContext
import ognl.ASTProperty
import ognl.ASTVarRef
import ognl.Node
import ognl.Ognl
import org.gcb.blah.general.GeneralProjectUtils
import java.util.Deque
import java.util.Queue

object MybatisParmProvider: PsiReferenceProvider() {
    private val PARAM_REGEX = Regex("""[#$]\{([a-zA-Z_$][a-zA-Z0-9_$]*)}""")
    override fun getReferencesByElement(
        element: PsiElement,
        p1: ProcessingContext
    ): Array<out PsiReference?> {
        val targetClass = resolveParameterTypeForElement(element) ?: return PsiReference.EMPTY_ARRAY
        val text = element.text
        val res = mutableListOf<PsiReference>()
        PARAM_REGEX.findAll(text).forEach { matchResult ->
            val innerGroup = matchResult.groups[1]
            val value = innerGroup?.value ?: return@forEach
            res.add(MybatisParamReference(element, TextRange(value.range.start, value.range.start + value.range.length), value.trimIndent(), targetClass))
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
                node.jjtGetChild(childIdx)?.let { res.add(it) }
            }
        }
        return res
    }
}

object MybatisXmlAttributeParamProvider: PsiReferenceProvider() {
    private val XML_ATTRIBUTE_PARAM = Regex("""""")
    override fun getReferencesByElement(p0: PsiElement, p1: ProcessingContext): Array<out PsiReference?> {
        val root = Ognl.parseExpression(p0.text) as Node
        OgnlUtils.bfs(root)
        return PsiReference.EMPTY_ARRAY
    }
}

class MybatisReferenceContributor: PsiReferenceContributor() {
    override fun registerReferenceProviders(rgstr: PsiReferenceRegistrar) {
        val mybatisSqlPattern = PlatformPatterns.psiElement(XmlTokenType.XML_DATA_CHARACTERS)
            // 过滤层 1：必须在 XML 文件中（极速失败，保护性能）
            .inFile(XmlPatterns.xmlFile())
            // 过滤层 2：向上遍历，必须被包裹在这些特定的标签中
            .inside(
                XmlPatterns.xmlTag().withName(
                    StandardPatterns.string().oneOf("select", "insert", "update", "delete", "sql")
                )
            )
        rgstr.registerReferenceProvider(mybatisSqlPattern, MybatisParmProvider)
        val mybatisXmlAttributePattern = PlatformPatterns.psiElement(XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN)
            .inFile(XmlPatterns.xmlFile())
            .inside(XmlPatterns.xmlAttribute().withName("test"))
        rgstr.registerReferenceProvider(mybatisXmlAttributePattern,
            MybatisXmlAttributeParamProvider)
    }
}

private fun resolveParameterTypeForElement(element: PsiElement): PsiClass? {
    val tag = PsiTreeUtil.getParentOfType(element, XmlTag::class.java) ?: return null
    if (tag.name != "select") {
        return null
    }
    val resultTypeStr = tag.getAttributeValue("parameterType") ?: return null
    return  GeneralProjectUtils.findClazzExistInProject(element.project, resultTypeStr)
}