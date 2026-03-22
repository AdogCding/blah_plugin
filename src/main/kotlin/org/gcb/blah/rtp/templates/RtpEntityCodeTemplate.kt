package org.gcb.blah.rtp.templates

import org.gcb.blah.rtp.entity.TableDefEntity

object RtpEntityCodeTemplate {
    fun getEntityCode(tableDef: TableDefEntity): String {
        return """
public class ${tableDef.tableName.toUpperCamelCase()} {
    ${tableDef.columns.joinToString("\n\t") { "private String ${it.toLowerCamelCase()};" }}
}
        """.trimIndent()
    }
}