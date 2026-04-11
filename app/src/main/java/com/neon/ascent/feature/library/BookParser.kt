package com.neon.ascent.feature.library

import android.content.Context
import nl.siegmann.epublib.epub.EpubReader
import org.jsoup.Jsoup
import java.io.InputStream

class BookParser(private val context: Context) {
    fun parseEpub(inputStream: InputStream, id: String): EBook {
        val book = EpubReader().readEpub(inputStream)
        val title = book.title ?: "Unknown Title"
        val author = if (book.metadata.authors.isNotEmpty()) {
            val firstAuthor = book.metadata.authors[0]
            "${firstAuthor.firstname} ${firstAuthor.lastname}".trim()
        } else {
            "Unknown Author"
        }
        val language = book.metadata.language ?: "en"

        // Use book.spine to get the reading order
        val chapters = book.spine.spineReferences.map { spineReference ->
            val resource = spineReference.resource
            val doc = Jsoup.parse(String(resource.data, charset(resource.inputEncoding ?: "UTF-8")))
            Chapter(
                title = resource.title ?: "Chapter",
                content = doc.body().html()
            )
        }

        return EBook(
            id = id,
            title = title,
            author = author,
            chapters = chapters,
            language = language
        )
    }
}
