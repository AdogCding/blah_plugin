package org.gcb.blah.mybatis.jump

data class MyBatisDmlSql(val sqlId: String, val namespace: String) {

}

fun MyBatisDmlSql.toFullName(): String {
    return "${this.namespace}.${this.sqlId}"
}