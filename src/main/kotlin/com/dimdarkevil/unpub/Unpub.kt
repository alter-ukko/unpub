package com.dimdarkevil.unpub

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.javalin.Javalin
import io.javalin.http.ContentType
import io.javalin.http.staticfiles.Location
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.net.URLConnection

object Unpub {
    private val log = LoggerFactory.getLogger(Unpub::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        val om = ObjectMapper(YAMLFactory())
            .registerKotlinModule()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val rootDir = File(HOME, "files/unpub_web")
        val books = om.readValue<List<BulkImport.BookMetadata>>(File(rootDir, "books.yaml"))
            .associateBy { it.id }
        val fileNameMap = URLConnection.getFileNameMap()
        var config = loadConfig(om)

        val app = Javalin.create { config ->
            config.http.asyncTimeout = 10_000L
            config.router.ignoreTrailingSlashes = true
            val x = Location.EXTERNAL
        }
        app.get("/") { ctx ->
            ctx.redirect("/index.html")
        }
        app.get("/index.html") { ctx ->
            val sort = ctx.queryParam("sort") ?: "title"
            val sb = StringBuilder()
            sb.appendLine(htmlTop)
            when (sort) {
                "author" -> {
                    sb.appendLine("<a href=\"index.html?sort=title\">[title sort]</a><a href=\"settings.html\">[settings]</a>")
                    sb.appendLine("<hr>")
                    books.values.sortedBy { it.author }
                        .groupBy { it.author }
                        .forEach { (author, bookMetas) ->
                            sb.appendLine("<h3>${author}</h3>")
                            sb.appendLine("<ul class=\"no-bullets\">")
                            bookMetas.sortedBy { it.title }.forEach {
                                sb.appendLine("<li><a href=\"/book/${it.id}/book.html\">${it.title}</a></li>")
                            }
                            sb.appendLine("</ul>")
                        }
                }
                else -> {
                    sb.appendLine("<a href=\"index.html?sort=author\">[author sort]</a><a href=\"settings.html\">[settings]</a>")
                    sb.appendLine("<hr>")
                    sb.appendLine("<ul class=\"no-bullets\">")
                    books.values.sortedBy { it.title }.forEach {
                        sb.appendLine("<li><a href=\"/book/${it.id}/book.html\">${it.title} - ${it.author}</a></li>")
                    }
                    sb.appendLine("</ul>")
                }
            }
            sb.appendLine(htmlBottom)
            ctx.html(sb.toString())
        }
        app.get("/settings.html") { ctx ->
            val sb = StringBuilder()
            sb.appendLine(htmlTop)
            sb.appendLine("<a href=\"index.html\">[home]</a>")
            sb.appendLine("<hr>")
            sb.appendLine(config.applyToForm(settingsForm))

            sb.appendLine(htmlBottom)
            ctx.html(sb.toString())
        }
        app.post("/settings") { ctx ->
            config = saveConfig(
                UnpubConfig(
                    backgroundColor = ctx.formParam("backgroundColor") ?: config.backgroundColor,
                    fontColor = ctx.formParam("fontColor") ?: config.fontColor,
                    fontSize = ctx.formParam("fontSize") ?: config.fontSize,
                    topMargin = ctx.formParam("topMargin") ?: config.topMargin,
                    sideMargin = ctx.formParam("sideMargin") ?: config.sideMargin,
                    paragraphSpacing = ctx.formParam("paragraphSpacing") ?: config.paragraphSpacing,
                    linkFontColor = ctx.formParam("linkFontColor") ?: config.linkFontColor,
                    textAlign = ctx.formParam("textAlign") ?: config.textAlign,
                ),
                om
            )
            ctx.redirect("/settings.html")
        }
        app.get("/style.css") { ctx ->
            ctx.contentType(ContentType.TEXT_CSS).result(config.applyToStyles(styles))
        }
        app.get("/book/{id}") { ctx ->
            val id = ctx.pathParam("id")
            ctx.redirect("/book/${id}/book.html")
        }
        app.get("/book/{id}/book.html") { ctx ->
            val id = ctx.pathParam("id")
            books[id]?.let { bookMeta ->
                ctx.html(File(rootDir, "${bookMeta.id}/book.html").readText(Charsets.UTF_8))
            } ?: ctx.status(404)
        }
        app.get("book/{id}/style.css") { ctx ->
            // handle specifically because we will customize here
            val id = ctx.pathParam("id")
            books[id]?.let { bookMeta ->
                ctx.contentType(ContentType.TEXT_CSS).result(config.applyToStyles(styles))
            } ?: ctx.status(404)
        }
        app.get("/book/{id}/*") { ctx ->
            val id = ctx.pathParam("id")
            books[id]?.let { bookMeta ->
                val filePath = ctx.req().requestURI.substring(6)
                val file = File(rootDir, filePath)
                if (file.exists()) {
                    val mimeType = fileNameMap.getContentTypeFor(file.name)
                    FileInputStream(file).let { inputStream ->
                        ctx.result(inputStream)
                    }
                } else {
                    ctx.status(404)
                }
            } ?: ctx.status(404)
        }
        app.get("/fonts/*") { ctx ->
            val filePath = ctx.req().requestURI
            FileInputStream(File(rootDir, filePath)).let { inputStream ->
                ctx.result(inputStream)
            }
        }
        app.get("*") { ctx ->
            log.info("unhandled request for ${ctx.req().requestURI}")
        }
        app.start(8000)
    }

    private fun loadConfig(om: ObjectMapper): UnpubConfig {
        val configDir = File(HOME, ".config/unpub")
        configDir.mkdirs()
        val configFile = File(configDir, "config.yaml")
        return if (configFile.exists()) {
            om.readValue<UnpubConfig>(configFile)
        } else {
            val config = UnpubConfig()
            om.writeValue(configFile, config)
            config
        }
    }

    private fun saveConfig(config: UnpubConfig, om: ObjectMapper): UnpubConfig {
        val configDir = File(HOME, ".config/unpub")
        configDir.mkdirs()
        val configFile = File(configDir, "config.yaml")
        om.writeValue(configFile, config)
        return config
    }
}