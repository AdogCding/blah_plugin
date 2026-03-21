package org.gcb.blah.rtp.templates

import org.gcb.blah.rtp.entity.TableDefEntity
import java.util.UUID

object RtpMybatisCodeTemplate {
    fun getSelectStmt(table: TableDefEntity): String {
        val selectId = UUID.randomUUID()
        return """
            <?xml version="1.0" encoding="UTF-8" ?>
            <!DOCTYPE mapper
                PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
                    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
            <mapper namespace="demo">
                <select id="$selectId" resultType="" parameterType="">
                        select ${table.columns.joinToString(", ")} 
                        from ${table.tableName}
                </select>
            </mapper>
        """.trimIndent()
    }
}