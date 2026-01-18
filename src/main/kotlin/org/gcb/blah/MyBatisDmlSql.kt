package org.gcb.blah

data class MyBatisDmlSql(val sqlId: String, val namespace: String) {

}

fun MyBatisDmlSql.toSqlString(): String {
    return "${this.namespace}.${this.sqlId}"
}