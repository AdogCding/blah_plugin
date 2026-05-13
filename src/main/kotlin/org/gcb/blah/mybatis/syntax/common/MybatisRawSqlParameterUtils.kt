package org.gcb.blah.mybatis.syntax.common

import com.intellij.platform.kernel.util.CommonInstructionSet
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiExpressionList
import com.intellij.psi.PsiExpressionStatement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.sql.formatter.model.COMMENT_TYPES

object MybatisRawSqlParameterUtils {
    private val PARAM_REGEX = Regex("""[#$]\{([a-zA-Z_$][a-zA-Z0-9_$]*)}""")

    fun matchParameters(sql: String): List<MatchResult> {
        return PARAM_REGEX.findAll(sql).toList()
    }
}

object MybatisMapKeyUtils {
    fun findMethodDeclaration(element: PsiElement): PsiMethod? {
        return PsiTreeUtil.findFirstParent(element) {
            it is PsiMethod
        } as? PsiMethod
    }

    fun findPutUsage(method:PsiMethod, key: String): PsiMethodCallExpression? {
        val methodCallExpressionList = PsiTreeUtil.findChildrenOfType(method, PsiMethodCallExpression::class.java)
        for (methodCallExpression in methodCallExpressionList) {
            val expression = methodCallExpression.methodExpression
            if (methodCallExpression.methodExpression.referenceName != "put") {
                continue
            }
            val qualifier = expression.qualifierExpression ?: return null
            // not map put
            if (!InheritanceUtil.isInheritor(qualifier.type, CommonClassNames.JAVA_UTIL_MAP)) {
                continue
            }
            val firstArgument = methodCallExpression.argumentList.expressions[0] as? PsiLiteralExpression ?: continue
            if (firstArgument.value == key) {
                return methodCallExpression
            }
        }
        return null
    }
}