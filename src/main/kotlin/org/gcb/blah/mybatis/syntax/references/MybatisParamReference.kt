package org.gcb.blah.mybatis.syntax.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import org.gcb.blah.mybatis.jump.PluginSettingState

class MybatisParamReference(element: PsiElement,
                            textRange: TextRange,
                            private val paramName:String,
                            private val targetClazz: PsiClass): PsiReferenceBase<PsiElement>(element, textRange) {
    override fun resolve(): PsiElement? {
        val field = targetClazz.allFields.find { it.name == paramName }
        if (field != null) return field

        val getterName = "get${paramName.replaceFirstChar { it.uppercase() }}"
        val method = targetClazz.allMethods.find { it.name == getterName }
        if (method != null) {
            return method
        }
        // 就会自动在 XML 里的这个词下面画上红色波浪线，提示 "Cannot resolve symbol 'xxx'"！
        return null
    }

    override fun isSoft(): Boolean {
        return !PluginSettingState.getInstance(element.project).isMybatisRefCheckStrict
    }

    override fun getVariants(): Array<out Any?> {
        return targetClazz.allFields.map { it.name }.toTypedArray()
    }
}