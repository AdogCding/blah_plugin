package org.gcb.blah.mybatis.syntax.common

import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiParameter
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiTreeUtil
import org.gcb.blah.mybatis.syntax.references.PsiParameterAndMethodCombination
import org.gcb.blah.mybatis.syntax.references.PsiParameterIndexAndMethodCallExpr
import org.jetbrains.letsPlot.core.spec.back.transform.bistro.util.map

object MybatisRawSqlParameterUtils {
    private val PARAM_REGEX = Regex("""[#$]\{([a-zA-Z_$][a-zA-Z0-9_$]*)}""")

    fun matchParameters(sql: String): List<MatchResult> {
        return PARAM_REGEX.findAll(sql).toList()
    }
}

object MybatisMapKeyUtils {
    /**
     * 找到DBUtils.selectList(SQL, map)
     * map的声明位置
     */
    fun findMethodMapParameterDeclaration(methodCallExpression: PsiMethodCallExpression): List<PsiMethod> {
        val paramMapDeclaration = findAndResolveMapParameterOfMethodCallExpr(methodCallExpression, 1)
        // start to search recursively
        if (paramMapDeclaration is PsiParameter) {
            val method = PsiTreeUtil.getParentOfType(paramMapDeclaration, PsiMethod::class.java) ?: return emptyList()
            return findOriginalInitMethodOfParameter(PsiParameterAndMethodCombination(method, paramMapDeclaration, method.parameterList.getParameterIndex(paramMapDeclaration)))
        }
        val r = PsiTreeUtil.getParentOfType(paramMapDeclaration, PsiMethod::class.java) ?: return emptyList()
        return listOf(r)
    }

    /**
     * DBUtil.select(xxx, map, ...)
     * locate map and resolve it
     * for example:
     * private void a(Map<String, String> map) {
     *  DBUtil.selectList(xxx, map, xxx)
     * }
     * this function will return parameter map
     */
    private fun findAndResolveMapParameterOfMethodCallExpr(methodCallExpr: PsiMethodCallExpression, index: Int): PsiElement? {
        val argumentExprList = methodCallExpr.argumentList.expressions
        // assume map is always second parameter of method call
        if (argumentExprList.size <= index) {
            return null
        }
        return argumentExprList[index].reference?.resolve()
    }





    private fun findOriginalInitMethodOfParameter(mapRef: PsiParameterAndMethodCombination): List<PsiMethod> {
        val visited = mutableSetOf<PsiMethodCallExpression>()
        val result = mutableListOf<PsiMethod>()
        val startMethod = PsiTreeUtil.getParentOfType(mapRef.parameter, PsiMethod::class.java) ?: return emptyList()
        val stack = ArrayDeque<PsiParameterIndexAndMethodCallExpr>()
        val usages = MethodReferencesSearch.search(startMethod).findAll()
        for (usage in usages) {
            val candidateMethodCallExpr = PsiTreeUtil.getParentOfType(usage.element, PsiMethodCallExpression::class.java)
            if (candidateMethodCallExpr != null) {
                stack.addLast(PsiParameterIndexAndMethodCallExpr(candidateMethodCallExpr, mapRef.index))
            }
        }
        while (stack.isNotEmpty()) {
            val parameterIndexAndMethodCallExpr = stack.removeLast()
            // 已经访问过这个MethodCallExpr
            if (!visited.add(parameterIndexAndMethodCallExpr.methodCallExpr)) {
                continue
            }
            val mapParameter = findAndResolveMapParameterOfMethodCallExpr(parameterIndexAndMethodCallExpr.methodCallExpr, parameterIndexAndMethodCallExpr.index)
            if (mapParameter is PsiParameter) {
                val method = PsiTreeUtil.getParentOfType(mapParameter, PsiMethod::class.java) ?: continue
                val index = method.parameterList.getParameterIndex(mapParameter)
                val moreUsages = MethodReferencesSearch.search(method).findAll()
                for (usage in moreUsages) {
                    val methodCallExpr = PsiTreeUtil.getParentOfType(usage.element, PsiMethodCallExpression::class.java) ?: continue
                    stack.addLast(PsiParameterIndexAndMethodCallExpr(methodCallExpr, index))
                }
            } else if (mapParameter is PsiLocalVariable) {
                val method = PsiTreeUtil.getParentOfType(mapParameter, PsiMethod::class.java) ?: continue
                result.add(method)
            }
        }
        return result
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