package me.pectics.papi.expansion.cmiex.utils

fun <E> List<E>.langf(strict: Boolean = false): String {
    return if (!strict) this.joinToString(":")
    else this.joinToString("\":\"", prefix = "\"", postfix = "\"")
}