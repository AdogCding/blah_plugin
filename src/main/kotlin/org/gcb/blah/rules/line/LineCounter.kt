package org.gcb.blah.rules.line

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil

class LineCounter : AbstractBaseJavaLocalInspectionTool() {
    @JvmField
    var limitLineCnt = 0
    var ignoreKeyword = "IgnoreLimit"

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethod(method: PsiMethod) {
                if (hasIgnoreComment(method)) {
                    return
                }
                val lineCount = method.body?.text?.lines()?.count() ?: return
                if (lineCount <= limitLineCnt) {
                    return
                }
                holder.registerProblem(
                    method.nameIdentifier ?: return,
                    "方法行数过长 ($lineCount 行，限制 $limitLineCnt 行)", LineCountExceedLimitFixer()
                )
            }

            private fun hasIgnoreComment(method: PsiMethod): Boolean {
                var prevBro = PsiTreeUtil.skipWhitespacesBackward(method)
                while (prevBro != null) {
                    if (prevBro is PsiComment && isIgnoreComment(prevBro)) {
                        return true
                    }
                }
                return false
            }

            private fun isIgnoreComment(prevBro: PsiComment): Boolean {
                val text = prevBro.text.trim().run {
                    ""
                }
                return text == ignoreKeyword
            }
        }
    }
}