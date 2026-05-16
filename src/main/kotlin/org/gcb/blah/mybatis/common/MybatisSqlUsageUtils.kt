package org.gcb.blah.mybatis.common

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiBinaryExpression
import com.intellij.psi.PsiDeclarationStatement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiField
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.impl.JavaConstantExpressionEvaluator
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import org.gcb.blah.mybatis.jump.MyBatisDmlSql
import org.gcb.blah.mybatis.jump.toFullName

object MybatisSqlUsageUtils {
    /**
     * 根据字符串找到sqlId的使用函数
     * 1. sqlId作为字符串的一部分出现在一个函数调用中
     * e.g. DBUtils.selectList(SQL_NS + "sqlId", ..)
     *
     * 2. sqlId经过拼接后作为完整的sqlId
     * e.g. private static final String SQL_ID = SQL_NS + "sqlId"
     *      DBUtils.selectList(SQL_ID, ...)
     *
     * 3. sqlId直接属于字符串的一部分
     *  e.g. private static final String SQL_ID = "sn+sqlId"
     *       DBUtils.selectList(SQL_ID, ...)
     */
    fun findMethodExpression(project: Project, toolClassName: String, myBatisDmlSql: MyBatisDmlSql): List<PsiMethodCallExpression> {
        val res = mutableListOf<PsiMethodCallExpression>()
        val scope = GlobalSearchScope.allScope(project)
        val psiSearchHelper = PsiSearchHelper.getInstance(project)
        psiSearchHelper.processElementsWithWord({ psiEl, _ ->
            if (psiEl.parent !is PsiLiteralExpression) {
                return@processElementsWithWord true
            }
            val literal = psiEl.parent as PsiLiteralExpression
            val trivialLiteralAndItsUsages = findLiteralAndItsUsage(literal, toolClassName, myBatisDmlSql)
            if (trivialLiteralAndItsUsages.isNotEmpty()) {
                res.addAll(trivialLiteralAndItsUsages)
            }
            val binaryExpressionAndItsUsages =
                findBinaryExpressionAndItsUsageWhenLiteralRefSqlId(literal, toolClassName, myBatisDmlSql)
            if (binaryExpressionAndItsUsages.isNotEmpty()) {
                res.addAll(binaryExpressionAndItsUsages)
            }
            val concatExprAndItsUsages = findConcatExpressionAndItsUsage(literal, toolClassName, myBatisDmlSql);
            if (concatExprAndItsUsages.isNotEmpty()) {
                res.addAll(concatExprAndItsUsages)
            }
            true
        }, scope, myBatisDmlSql.sqlId, UsageSearchContext.IN_STRINGS, true)
        return res
    }

    private fun findLiteralAndItsUsage(
        literal: PsiLiteralExpression,
        toolClassName: String,
        sqlId: MyBatisDmlSql
    ): List<PsiMethodCallExpression> {
        if (literal.value != sqlId.toFullName()) {
            return emptyList()
        }
        val res = mutableListOf<PsiMethodCallExpression>()
        res.addAll(getDirectUsages(literal, toolClassName))
        res.addAll(getLocalVariableUsages(literal, toolClassName))
        res.addAll(getSqlFieldUsages(literal, toolClassName))
        return res
    }

    /**
     * DBUtils.selectList("ns+sqlId", ...)
     */
    private fun getDirectUsages(expression: PsiExpression, toolClassName: String): List<PsiMethodCallExpression> {
        val methodCall = PsiTreeUtil.getParentOfType(expression, PsiMethodCallExpression::class.java)
        if (methodCall?.resolveMethod()?.containingClass?.qualifiedName == toolClassName) {
            return listOf(methodCall)
        }
        return emptyList()
    }

    /**
     * String sqlId = "Your Sql Id";
     * DBUtils.select(sqlId, param);
     */
    private fun getLocalVariableUsages(
        expression: PsiExpression,
        toolClassName: String
    ): List<PsiMethodCallExpression> {
        val declareStatement =
            PsiTreeUtil.getParentOfType(expression, PsiDeclarationStatement::class.java) ?: return emptyList()
        val localVariable =
            PsiTreeUtil.getChildOfType(declareStatement, PsiLocalVariable::class.java) ?: return emptyList()
        return getExpressionRefByToolClass(localVariable, toolClassName)
    }


    private fun getExpressionRefByToolClass(base: PsiElement, toolClassName: String): List<PsiMethodCallExpression> {
        val queryResult = ReferencesSearch.search(base)
        val res = mutableListOf<PsiMethodCallExpression>()
        queryResult.forEach { psiRef ->
            if (psiRef !is PsiReferenceExpression) {
                return@forEach
            }
            val methodCall = PsiTreeUtil.getParentOfType(psiRef, PsiMethodCallExpression::class.java)
            if (methodCall?.resolveMethod()?.containingClass?.qualifiedName == toolClassName) {
                res.add(methodCall)
            }
        }
        return res
    }

    /**
     * public class Foo {
     *  private static final String SQL_ID = "xxx.xxxx";
     *  xxx() {
     *      DBUtils.selectList(SQL_ID, xxxx)
     *  }
     * }
     */
    private fun getSqlFieldUsages(expression: PsiExpression, toolClassName: String): List<PsiMethodCallExpression> {
        val sqlIdField = PsiTreeUtil.getParentOfType(expression, PsiField::class.java) ?: return emptyList()
        return getExpressionRefByToolClass(sqlIdField, toolClassName)
    }


    /**
     * find sql id which is calculation result of binary expression
     * like:
     * {
     *  String sqlId = ns + "id"
     * }
     * after find out if the binary expression is target sql id
     * 1. if this binary expression is assigned to a class field, then getSqlFieldUsages,
     * 2. if this binary expression is assigned to a local variable, then getLocalVariableUsages
     * 3. if this binary expression is used as a function parameter directly, then call getDirectUsages
     */
    private fun findBinaryExpressionAndItsUsageWhenLiteralRefSqlId(
        literal: PsiLiteralExpression,
        toolClassName: String,
        sqlId: MyBatisDmlSql
    ): List<PsiMethodCallExpression> {
        val res = mutableListOf<PsiMethodCallExpression>()
        // check if it is a binary expression
        val binaryExpression =
            PsiTreeUtil.getParentOfType(literal, PsiBinaryExpression::class.java) ?: return emptyList()
        val binaryExprEvalRes = JavaConstantExpressionEvaluator.computeConstantExpression(binaryExpression, false)
        if (binaryExprEvalRes != sqlId.toFullName()) {
            return emptyList()
        }
        val sqlFieldUsages = getSqlFieldUsages(binaryExpression, toolClassName)
        val localVariableUsages = getLocalVariableUsages(binaryExpression, toolClassName)
        val directUsages = getDirectUsages(binaryExpression, toolClassName)
        res.addAll(sqlFieldUsages)
        res.addAll(localVariableUsages)
        res.addAll(directUsages)
        return res
    }

    /**
     * 支持使用String.concat进行字符串拼接
     */
    private fun findConcatExpressionAndItsUsage(
        literal: PsiLiteralExpression,
        toolClassName: String,
        sqlId: MyBatisDmlSql
    ): List<PsiMethodCallExpression> {
        val methodCall = PsiTreeUtil.getParentOfType(literal, PsiMethodCallExpression::class.java) ?: return emptyList()
        if (!isConcatExprEqual(methodCall, sqlId)) {
            return emptyList()
        }
        val res = mutableListOf<PsiMethodCallExpression>()
        //位于参数列表的引用，要从方法调用开始寻找
        val localVarUsages = getLocalVariableUsages(methodCall, toolClassName)
        val directUsages = getDirectUsages(methodCall, toolClassName)
        val sqlFieldUsages = getSqlFieldUsages(methodCall, toolClassName)
        res.addAll(localVarUsages)
        res.addAll(directUsages)
        res.addAll(sqlFieldUsages)
        return res
    }

    /**
     * 检查这个methodCall是不是xxx.concat("sqlId")
     */
    private fun isConcatExprEqual(root: PsiMethodCallExpression, sqlId: MyBatisDmlSql): Boolean {
        val stack = ArrayDeque<String>()
        var methodCallExpr: PsiMethodCallExpression? = root
        while (methodCallExpr != null) {
            val methodCallId = PsiTreeUtil.getChildOfType(methodCallExpr.methodExpression, PsiIdentifier::class.java)
            if (methodCallId?.text != "concat") {
                break
            }
            val sqlPart = PsiTreeUtil.getChildOfType(methodCallExpr.argumentList, PsiLiteralExpression::class.java) ?: break
            stack.addLast(sqlPart.value as String)
            val nextMethodCall =
                PsiTreeUtil.getChildOfType(methodCallExpr.methodExpression, PsiMethodCallExpression::class.java)
            if (nextMethodCall == null) {
                val helper = JavaPsiFacade.getInstance(root.project).constantEvaluationHelper
                val caller =
                    PsiTreeUtil.getChildOfType(methodCallExpr.methodExpression, PsiExpression::class.java)
                        ?: break
                val str = helper.computeConstantExpression(caller) as? String ?: return false
                stack.addLast(str)
            }
            methodCallExpr = nextMethodCall
        }
        if (stack.isEmpty()) {
            return false
        }
        val sqlIdOfConcat = StringBuilder()
        while (stack.isNotEmpty()) {
            sqlIdOfConcat.append(stack.removeLast())
        }
        return sqlIdOfConcat.toString() == sqlId.toFullName()
    }

    fun findSqlIdOfXmlRawSql(mybatisDmlTag: XmlTag): MyBatisDmlSql? {
        val sqlId = mybatisDmlTag.getAttributeValue("id")
        if (mybatisDmlTag.parent == null || mybatisDmlTag.parent !is XmlTag) {
            return null
        }
        val mapperTag = mybatisDmlTag.parent as XmlTag

        if (mapperTag.name != "mapper" || mapperTag.getAttributeValue("namespace").isNullOrBlank()) {
            return null
        }
        val ns = mapperTag.getAttributeValue("namespace")
        if (sqlId.isNullOrBlank() || ns.isNullOrBlank()) {
            return null
        }
        return MyBatisDmlSql(sqlId, ns)
    }


    fun isMybatisDmlTag(xmlTag: PsiElement): Boolean {
        if (xmlTag !is XmlTag) {
            return false
        }
        val tagName = xmlTag.name
        return tagName in setOf("select", "insert", "delete", "update")
    }
}