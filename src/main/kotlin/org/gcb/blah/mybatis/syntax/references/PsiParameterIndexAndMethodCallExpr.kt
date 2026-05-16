package org.gcb.blah.mybatis.syntax.references

import com.intellij.psi.PsiMethodCallExpression

data class PsiParameterIndexAndMethodCallExpr(val methodCallExpr: PsiMethodCallExpression, val index: Int)
