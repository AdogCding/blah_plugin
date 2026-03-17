package org.gcb.blah.rtp

import org.gcb.blah.rtp.entity.TableDefEntity

object RtpCodeTemplate {
    fun getSelectStmt(table: TableDefEntity): String {
        return """
            <?xml version="1.0" encoding="UTF-8" ?>
            <!DOCTYPE mapper
                    PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
                    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
            <mapper namespace="demo">
                <select>
                            select ${table.columns.joinToString(", ")} from ${table.tableName}
                </select>
            </mapper>
        """.trimIndent()
    }

}