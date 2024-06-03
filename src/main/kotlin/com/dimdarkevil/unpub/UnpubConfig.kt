package com.dimdarkevil.unpub

data class UnpubConfig(
    val backgroundColor: String = "#ffffff",
    val fontColor: String = "#000000",
    val fontSize: String = "16pt",
    val topMargin: String = "0.5em",
    val sideMargin: String = "2em",
    val paragraphSpacing: String = "1em",
    val linkFontColor: String = "#0000ff",
    val textAlign: String = "left",
)

fun UnpubConfig.applyToStyles(styles: String): String {
    return styles
        .replace("{backgroundColor}", backgroundColor)
        .replace("{fontColor}", fontColor)
        .replace("{fontSize}", fontSize)
        .replace("{topMargin}", topMargin)
        .replace("{sideMargin}", sideMargin)
        .replace("{paragraphSpacing}", paragraphSpacing)
        .replace("{linkFontColor}", linkFontColor)
        .replace("{textAlign}", textAlign)
}

fun UnpubConfig.applyToForm(form: String): String {
    return form
        .replace("{backgroundColor}", backgroundColor)
        .replace("{fontColor}", fontColor)
        .replace("{fontSize}", fontSize)
        .replace("{topMargin}", topMargin)
        .replace("{sideMargin}", sideMargin)
        .replace("{paragraphSpacing}", paragraphSpacing)
        .replace("{linkFontColor}", linkFontColor)
        .replace("{textAlign}", textAlign)
}
