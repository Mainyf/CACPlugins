package io.github.mainyf.itemskillsplus

fun String.tvar(name: String, text: String): String {
    return replace("{${name}}", text)
}