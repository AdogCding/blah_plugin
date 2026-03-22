package org.gcb.blah.rtp.templates

import org.gcb.blah.rtp.entity.TableDefEntity
import java.util.UUID

object RtpMybatisCodeTemplate {
    fun getMyBatisStmts(table: TableDefEntity): String {
        return """
            <?xml version="1.0" encoding="UTF-8" ?>
            <!DOCTYPE mapper
                PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
                    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
            <mapper namespace="demo">
                ${getSelectStmt(table)}
                ${getInsertionStmt(table)}
                ${getUpdateStmt(table)}
            </mapper>
        """.trimIndent()
    }
    fun getSelectStmt(table: TableDefEntity): String {
        val selectId = UUID.randomUUID()
        return """
                <select id="$selectId" resultType="" parameterType="">
                        select ${table.columns.joinToString(", ")} 
                        from ${table.tableName}
                </select>
        """.trimIndent()
    }

    fun getInsertionStmt(table: TableDefEntity): String {
        return """
            <insert id="insert${table.tableName.toUpperCamelCase()}">
                insert into ${table.tableName} (${table.columns.joinToString(", ")})
                values (${table.columns.joinToString(", ") { "#{${it.toLowerCamelCase()}}" }})
            </insert>
        """.trimIndent()
    }

    fun getUpdateStmt(table: TableDefEntity): String {
        return """
            <update id="update${table.tableName.toUpperCamelCase()}">
                update ${table.tableName} set ${table.columns.joinToString(", ") {"$it = #{${it.toLowerCamelCase()}}"} }
                where ${table.primaryKeys.joinToString(", ") {"$it = #{${it.toLowerCamelCase()}}"}}
            </update>
        """.trimIndent()
    }
}