package org.gcb.blah.rtp

import org.gcb.blah.rtp.entity.TableDefEntity

class RtpCodeTemplate {
    fun getSelectStmt(table: TableDefEntity): String {
        return """
            select ${table.columns.joinToString { "," }} from ${table.tableName}
        """.trimIndent()
    }

}