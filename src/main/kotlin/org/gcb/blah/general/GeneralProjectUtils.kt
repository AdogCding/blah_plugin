package org.gcb.blah.general

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope

object GeneralProjectUtils {
    fun findClazzExistInProject(project: Project, clazzStr: String): PsiClass? {
        val psiFacade = JavaPsiFacade.getInstance(project)
        val scope = GlobalSearchScope.projectScope(project)
        return psiFacade.findClass(clazzStr, scope)
    }
}