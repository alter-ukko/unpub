package com.dimdarkevil.unpub

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.jsoup.Jsoup
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.StringWriter
import java.net.URLDecoder
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

object EpubImport {
    private val multiSpace = Regex("\\_+")

    @JvmStatic
    fun main(args: Array<String>) {
        val om = ObjectMapper(YAMLFactory())
            .registerKotlinModule()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        val srcDir = File(HOME, "files/calibre")
        val unzipDir = File(HOME, "files/unpub")
        if (unzipDir.exists()) unzipDir.deleteRecursively()
        unzipDir.mkdirs()
        val destDir = File(HOME, "files/unpub_web")
        if (destDir.exists()) destDir.deleteRecursively()
        destDir.mkdirs()

        unzipAllEpubFiles(srcDir, unzipDir)

        unzipDir.listFiles().filter { it.isDirectory && !it.name.startsWith(".") }.map {
            processUnzippedEpub(it, destDir)
        }.let { bookList ->
            val metaFile = File(destDir, "books.yaml")
            om.writeValue(metaFile, bookList)
        }
    }

    fun processUnzippedEpub(rootDir: File, destDir: File): BookMetadata {
        val finalDir = File(destDir, rootDir.name)
        finalDir.mkdirs()
        println(rootDir.name)
        val containerFile = File(rootDir, "META-INF/container.xml")
        if (!containerFile.exists()) throw RuntimeException("container file ${containerFile.canonicalPath} does not exist")
        val containerXml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(containerFile)
        val rootFileNodes = containerXml.getElementsByTagName("rootfile")
        if (rootFileNodes.length == 0 || rootFileNodes.length > 1) throw RuntimeException("there are ${rootFileNodes.length} rootfiles in the epub")
        val contentPathNode = rootFileNodes.item(0).attributes.getNamedItem("full-path") ?: throw RuntimeException("no full-path attribute in rootfile element")
        val contentFile = File(rootDir, contentPathNode.nodeValue)
        if (!contentFile.exists()) throw RuntimeException("content file ${contentFile.canonicalPath} does not exist")
        val contentDir = contentFile.parentFile
        val contentXml =  DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(contentFile)
        val metadataNode = contentXml.getElementsByTagName("metadata")
        if (metadataNode.length == 0 || metadataNode.length > 1) throw RuntimeException("there are ${metadataNode.length} metadata tags in the content file")
        val bookMeta = BookMetadata(rootDir.name)
        metadataNode.item(0).childNodes.asSequence().filter { it.nodeName.startsWith("dc:") }.forEach { node ->
            when (node.nodeName) {
                "dc:title" -> bookMeta.title = node.textContent
                "dc:description" -> bookMeta.description = node.textContent
                "dc:publisher" -> bookMeta.publisher = node.textContent
                "dc:date" -> bookMeta.date = node.textContent.substring(0, 10)
                "dc:creator" -> {
                    val fullName = node.attribVal("opf:file-as") ?: node.textContent
                    val (firstName, lastName) = fullName.toFirstLast()
                    val role = node.attribVal("opf:role") ?: "aut"
                    bookMeta.creators.add(
                        CreatorRec(
                            name = fullName,
                            firstName = firstName,
                            lastName = lastName,
                            role = role
                        )
                    )
                }
                "dc:subject" -> bookMeta.subjects.add(node.textContent)
            }
        }
        val autAuthor = bookMeta.creators.filter { it.role == "aut" }
        bookMeta.author = if (autAuthor.isNotEmpty()) autAuthor.joinToString { it.name } else bookMeta.creators.joinToString { it.name }
        val manifestNode = contentXml.getElementsByTagName("manifest")
        if (manifestNode.length == 0 || manifestNode.length > 1) throw RuntimeException("there are ${manifestNode.length} manifest tags in the content file")
        val items = manifestNode.item(0).childNodes.asSequence().filter { it.nodeName == "item" }.map { node ->
            val fileName = URLDecoder.decode(node.attribVal("href") ?: throw RuntimeException("no href attrib in item node"), Charsets.UTF_8)
            ItemRec(
                id = node.attribVal("id") ?: throw RuntimeException("no id attrib in item node"),
                fileName = fileName,
                mediaType = node.attribVal("media-type") ?: throw RuntimeException("no href media-type in item node"),
            )
        }
        val itemMap = items.associateBy { it.id }
        val filenameMap = items.associateBy { it.fileName.split("/").last() }
        val spineNode = contentXml.getElementsByTagName("spine")
        if (spineNode.length == 0 || spineNode.length > 1) throw RuntimeException("there are ${spineNode.length} spine tags in the content file")

        StringWriter().use { sw ->
            sw.appendLine(htmlTop.replace("{title}", bookMeta.title))
            spineNode.item(0).childNodes.asSequence().filter { it.nodeName == "itemref" }.forEach { node ->
                val id = node.attribVal("idref") ?: throw RuntimeException("no idref attrib in itemref node")
                val item = itemMap[id] ?: throw RuntimeException("item not found for id ${id}")
                if (item.mediaType == "application/xhtml+xml") {
                    val itemFile = File(contentDir, item.fileName)
                    if (!itemFile.exists()) throw RuntimeException("item file ${itemFile.canonicalPath} does not exist")
                    val jdoc = Jsoup.parse(itemFile)
                    val body = jdoc.body()
                    // fix anchor links
                    body.getElementsByTag("a").forEach { atag ->
                        val href = atag.attr("href")
                        val hashPos = href.indexOf('#')
                        if (hashPos > 0) {
                            atag.attr("href", href.substring(hashPos))
                        } else if (href.isNotEmpty() && !href.startsWith("#")) {
                            filenameMap[href]?.let {
                                atag.attr("href", "#${it.id}")
                            }
                        }
                    }
                    // fix relative images
                    body.getElementsByTag("img").forEach { itag ->
                        val src = itag.attr("src")
                        val startPos = if (src.startsWith("../")) {
                            3
                        } else if (src.startsWith("./")) {
                            2
                        } else if (src.startsWith("/")) {
                            1
                        } else {
                            0
                        }
                        if (startPos > 0) {
                            itag.attr("src", src.substring(startPos))
                        }
                    }
                    sw.appendLine("<a id=\"$id\"></a>")
                    sw.appendLine(body.html())
                }
            }
            sw.appendLine(htmlBottom)
            File(finalDir, "book.html").writeText(sw.toString())
        }
        File(finalDir, "style.css").writeText(styles)
        items.filter { it.mediaType != "application/xhtml+xml" && it.mediaType != "text/css" }.forEach { item ->
            val assetSrcFile = File(contentDir, item.fileName)
            if (assetSrcFile.exists()) {
                val assetDestFile = File(finalDir, item.fileName)
                assetDestFile.parentFile.mkdirs()
                assetSrcFile.copyTo(assetDestFile, true)
            }
        }
        return bookMeta
    }

    fun unzipAllEpubFiles(srcDir: File, dstDir: File) {
        dstDir.mkdirs()
        srcDir.walkTopDown().filter { it.extension == "epub" }.forEach { f ->
            unzipEpubFile(f, dstDir)
        }
    }

    // returns the root directory where the unzipped epub file was placed
    fun unzipEpubFile(f: File, dstRootDir: File): File {
        val cleanName = cleanName(f)
        println("-=-= $cleanName")
        val dstDir = File(dstRootDir, cleanName)
        dstDir.mkdirs()
        val buffer =  ByteArray(65536)
        ZipInputStream(FileInputStream(f)).use { zis ->
            var zipEntry = zis.nextEntry
            while (zipEntry != null) {
                if (!zipEntry.isDirectory) {
                    val outFile = File(dstDir, zipEntry.name)
                    outFile.parentFile.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        var len = zis.read(buffer)
                        while (len > 0) {
                            fos.write(buffer, 0, len)
                            len = zis.read(buffer)
                        }
                    }
                }
                zipEntry = zis.nextEntry
            }
        }
        return dstDir
    }

    fun cleanName(f: File): String {
        return f.nameWithoutExtension.replace(' ', '_').filter { it == '_' || it.isDigit() || it.isLetter() }.replace(multiSpace, "_")
    }

    fun NodeList.asSequence(): Sequence<Node> {
        val nl = this
        return sequence {
            (0 ..< nl.length).forEach { idx ->
                yield(nl.item(idx))
            }
        }
    }

    fun Node.attribVal(attribName: String): String? {
        return this.attributes.getNamedItem(attribName)?.nodeValue
    }

    fun String.toFirstLast(): Pair<String,String> {
        val commaPos = this.indexOf(',')
        return if (commaPos > 0) {
            Pair(this.substring(commaPos+1).trim(), this.substring(0, commaPos).trim())
        } else {
            Pair("", this)
        }
    }

    data class ItemRec(
        val id: String,
        val fileName: String,
        val mediaType: String,
    )
}