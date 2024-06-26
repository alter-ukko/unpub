package com.dimdarkevil.unpub

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.javalin.Javalin
import io.javalin.config.SizeUnit
import io.javalin.http.ContentType
import io.javalin.util.FileUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.net.URLConnection
import java.nio.file.Files

object Unpub {
    private val log = LoggerFactory.getLogger(Unpub::class.java)
    private val serverConfigFile = File(HOME, ".config/unpub/unpub.yaml")

    @JvmStatic
    fun main(args: Array<String>) {
        val om = ObjectMapper(YAMLFactory())
            .registerKotlinModule()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val serverConfig = loadServerConfig(om)
        val rootDir = File(serverConfig.webRootDir)
        var books = loadBooks(rootDir, om)
        val fileNameMap = URLConnection.getFileNameMap()
        var config = loadConfig(om)
        val tmpDir = Files.createTempDirectory("unpub").toFile()
        log.info("-=-= temp directory is ${tmpDir.absolutePath}")

        val app = Javalin.create { config ->
            config.http.asyncTimeout = 10_000L
            config.router.ignoreTrailingSlashes = true
            config.jetty.multipartConfig.cacheDirectory(tmpDir.absolutePath) //where to write files that exceed the in memory limit
            config.jetty.multipartConfig.maxFileSize(100, SizeUnit.MB) //the maximum individual file size allowed
            config.jetty.multipartConfig.maxInMemoryFileSize(10, SizeUnit.MB) //the maximum file size to handle in memory
            config.jetty.multipartConfig.maxTotalRequestSize(1, SizeUnit.GB)
        }.events { event ->
            event.serverStopped {
                println("app closed")
                tmpDir.deleteRecursively()
            }
        }
        app.get("/") { ctx ->
            ctx.redirect("/index.html")
        }
        app.get("/index.html") { ctx ->
            val sort = ctx.queryParam("sort") ?: "title"
            val sb = StringBuilder()
            sb.appendLine(htmlTop)
            val menuItems = "<a href=\"settings.html\">[settings]</a><a href=\"upload.html\">[upload]</a>"
            when (sort) {
                "author" -> {
                    sb.appendLine("<a href=\"index.html?sort=title\">[title sort]</a>$menuItems")
                    sb.appendLine("<hr>")
                    sb.appendLine("<table>")
                    books.values.sortedBy { it.author }
                        .groupBy { it.author }
                        .forEach { (author, bookMetas) ->
                            sb.appendLine("<tr><th colspan=\"2\">${author}</th></tr>")
                            bookMetas.sortedBy { it.title }.forEach {
                                sb.appendLine("<tr>")
                                sb.appendLine("<td><a href=\"/book/${it.id}/book.html\" class=\"link-button\">read</a><a href=\"/book/${it.id}/edit.html\" class=\"link-button\">edit</a></td><td>${it.title}</td>")
                                sb.appendLine("</tr>")
                            }
                        }
                    sb.appendLine("</table>")
                }
                else -> {
                    sb.appendLine("<a href=\"index.html?sort=author\">[author sort]</a>$menuItems")
                    sb.appendLine("<hr>")
                    sb.appendLine("<table>")
                    books.values.sortedBy { it.sortTitle }.forEach {
                        sb.appendLine("<tr>")
                        sb.appendLine("<td style=\"min-width:8em;\"><a href=\"/book/${it.id}/book.html\" class=\"link-button\">read</a><a href=\"/book/${it.id}/edit.html\" class=\"link-button\">edit</a></td><td>${it.title}</td><td>${it.author}</td>")
                        sb.appendLine("</tr>")
                    }
                    sb.appendLine("</table>")
                }
            }
            sb.appendLine(htmlBottom)
            ctx.html(sb.toString())
        }
        app.get("/settings.html") { ctx ->
            val sb = StringBuilder()
            sb.appendLine(htmlTop)
            sb.appendLine("<a href=\"/index.html\">[home]</a>")
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
        app.get("/upload.html") { ctx ->
            val sb = StringBuilder()
            sb.appendLine(htmlTop)
            sb.appendLine("<a href=\"/index.html\">[home]</a>")
            sb.appendLine("<hr>")
            sb.appendLine(config.applyToForm(uploadForm))

            sb.appendLine(htmlBottom)
            ctx.html(sb.toString())
        }
        app.post("/upload") { ctx ->
            val messages = mutableListOf<String>()
            val metas = ctx.uploadedFiles().mapNotNull { uploadedFile ->
                if (uploadedFile.extension().lowercase() == ".epub") {
                    try {
                        val epubFile = File(tmpDir, uploadedFile.filename())
                        FileUtil.streamToFile(uploadedFile.content(), epubFile.absolutePath)
                        val unzipDir = EpubImport.unzipEpubFile(epubFile, tmpDir)
                        val bookMeta = EpubImport.processUnzippedEpub(unzipDir, rootDir)
                        messages.add("successfully imported ${bookMeta.title} by ${bookMeta.author}")
                        bookMeta
                    } catch (e: Exception) {
                        messages.add("error importing ${uploadedFile.filename()}: ${e.message}")
                        null
                    }
                } else {
                    messages.add("error importing ${uploadedFile.filename()}: not an epub file")
                    null
                }
            }
            if (metas.isNotEmpty()) {
                books = books.plus(metas.map { it.id to it })
                saveBooks(books.values, rootDir, om)
            }
            val sb = StringBuilder()
            sb.appendLine(htmlTop)
            sb.appendLine("<a href=\"/index.html\">[home]</a>")
            sb.appendLine("<hr>")
            sb.appendLine("<ul class=\"no-bullets\">")
            messages.forEach { msg ->
             sb.appendLine("<li>${msg}</li>")
            }
            sb.appendLine("</ul>")
            sb.appendLine(htmlBottom)
            ctx.html(sb.toString())
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
        app.get("/book/{id}/edit.html") { ctx ->
            val id = ctx.pathParam("id")
            books[id]?.let { bookMeta ->
                val sb = StringBuilder()
                sb.appendLine(htmlTop)
                sb.appendLine("<a href=\"/index.html\">[home]</a>")
                sb.appendLine("<hr>")
                sb.appendLine(bookMeta.applyToForm(editForm))
                sb.appendLine(htmlBottom)
                ctx.html(sb.toString())
            } ?: ctx.status(404)
        }
        app.post("/book/{id}/edit") { ctx ->
            val id = ctx.pathParam("id")
            books[id]?.let { bookMeta ->
                val newMeta = bookMeta.copy(
                    title = ctx.formParam("title") ?: bookMeta.title,
                    sortTitle = ctx.formParam("sortTitle") ?: bookMeta.sortTitle,
                    author = ctx.formParam("author") ?: bookMeta.author,
                    publisher = ctx.formParam("publisher") ?: bookMeta.publisher,
                    date = ctx.formParam("date") ?: bookMeta.date
                )
                books = books.plus(id to newMeta)
                saveBooks(books.values, rootDir, om)
            } ?: ctx.status(404)
            ctx.redirect("/index.html")
        }
        app.get("/book/{id}/edit_style.html") { ctx ->
            val id = ctx.pathParam("id")
            books[id]?.let { bookMeta ->
                val customCss = File(rootDir, "styles/${id}.css").let {
                    if (it.exists()) {
                        it.readText(Charsets.UTF_8)
                    } else {
                        styles
                    }
                }
                val sb = StringBuilder()
                sb.appendLine(htmlTop)
                sb.appendLine("<a href=\"/index.html\">[home]</a>")
                sb.appendLine("<hr>")
                sb.appendLine(bookMeta.applyToForm(editStyleForm).replace("{style}", customCss))
                sb.appendLine(htmlBottom)
                ctx.html(sb.toString())
            } ?: ctx.status(404)
        }
        app.post("/book/{id}/edit-style") { ctx ->
            val id = ctx.pathParam("id")
            books[id]?.let { bookMeta ->
                val newStyle = ctx.formParam("customStyle") ?: styles
                val customCssFile = File(rootDir, "styles/${id}.css")
                if (!customCssFile.exists()) {
                    customCssFile.parentFile.mkdirs()
                }
                customCssFile.writeText(newStyle, Charsets.UTF_8)
            } ?: ctx.status(404)
            ctx.redirect("/book/${id}/book.html")
        }
        app.get("/book/{id}/style.css") { ctx ->
            // handle specifically because we will customize here
            val id = ctx.pathParam("id")
            books[id]?.let { bookMeta ->
                val customCssFile = File(rootDir, "styles/${id}.css")
                if (customCssFile.exists()) {
                    ctx.contentType(ContentType.TEXT_CSS).result(config.applyToStyles(customCssFile.readText(Charsets.UTF_8)))
                } else {
                    ctx.contentType(ContentType.TEXT_CSS).result(config.applyToStyles(styles))
                }
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
        app.start(serverConfig.webServerPort)
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

    private fun loadBooks(rootDir: File, om: ObjectMapper): Map<String,BookMetadata> {
        return om.readValue<List<BookMetadata>>(File(rootDir, "books.yaml"))
            .associateBy { it.id }
    }

    private fun saveBooks(books: Collection<BookMetadata>, rootDir: File, om: ObjectMapper) {
        om.writeValue(File(rootDir, "books.yaml"), books)
    }

    private fun loadServerConfig(om: ObjectMapper): UnpubServerConfig {
        return if (serverConfigFile.exists()) {
            om.readValue<UnpubServerConfig>(serverConfigFile)
        } else {
            serverConfigFile.parentFile.mkdirs()
            val serverConfig = UnpubServerConfig(
                File(HOME, "unpub_web").absolutePath,
                8000
            )
            om.writeValue(serverConfigFile, serverConfig)
            serverConfig
        }
    }

    private fun saveServerConfig(serverConfig: UnpubServerConfig, om: ObjectMapper) {
        om.writeValue(serverConfigFile, serverConfig)
    }
}