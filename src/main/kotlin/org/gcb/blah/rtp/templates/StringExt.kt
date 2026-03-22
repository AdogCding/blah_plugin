package org.gcb.blah.rtp.templates

import com.google.common.base.CaseFormat


fun String.toUpperCamelCase(): String {
    return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this)
}

fun String.toLowerCamelCase(): String {
    return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, this)
}

