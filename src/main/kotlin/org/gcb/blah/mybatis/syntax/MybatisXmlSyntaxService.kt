package org.gcb.blah.mybatis.syntax;

import com.intellij.psi.xml.XmlTag


object MybatisXmlSyntaxService {
    fun collectUsedFieldsOfParameterType(psi: XmlTag): List<String> {
        return emptyList()
    }

    fun collectUsedFieldsOfParameterTypeOfUpdate(psi: XmlTag): List<String> {
        return emptyList()
    }

    fun collectUssFieldsOfParameterTypeOfDelete(psi: XmlTag): List<String> {
        return emptyList()
    }

    fun collectUsedFieldOfParameterTypeOfInsert(psi: XmlTag): List<String> {
        return emptyList()
    }
}

object SelectStmtFieldOfParameterTypeCollector {
    fun collect(xmlTag: XmlTag): List<String> {
        // examine <if>
        // examine <where>
        // examine <foreach>
        return emptyList()
    }

    fun examineIfTag(): List<String> {
        return emptyList()
    }

    fun examineWhereTag(): List<String> {
        return emptyList()
    }

    fun examineForEachTag(): List<String> {
        return emptyList()
    }
}
