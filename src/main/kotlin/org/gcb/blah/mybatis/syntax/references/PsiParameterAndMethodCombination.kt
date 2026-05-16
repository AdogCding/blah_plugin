package org.gcb.blah.mybatis.syntax.references

import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter

data class PsiParameterAndMethodCombination(val method: PsiMethod, val parameter: PsiParameter, val index: Int)