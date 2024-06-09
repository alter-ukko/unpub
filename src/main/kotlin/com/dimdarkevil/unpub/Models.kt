package com.dimdarkevil.unpub

data class BookMetadata(
    val id: String,
    var title: String = "",
    var sortTitle: String = title,
    var author: String = "",
    var description: String = "",
    var publisher: String = "",
    var date: String = "",
    var creators: MutableList<CreatorRec> = mutableListOf(),
    var subjects: MutableList<String> = mutableListOf(),
)

data class CreatorRec(
    val name: String,
    val firstName: String,
    val lastName: String,
    val role: String
)

fun BookMetadata.applyToForm(form: String): String {
    return form
        .replace("{id}", id)
        .replace("{title}", title)
        .replace("{sortTitle}", sortTitle)
        .replace("{author}", author)
        .replace("{description}", description)
        .replace("{publisher}", publisher)
        .replace("{date}", date)
}