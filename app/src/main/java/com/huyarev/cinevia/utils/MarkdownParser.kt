package com.huyarev.cinevia.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

/**
 * Compose için hafif (Lightweight) Markdown Dönüştürücü.
 * Sadece **Kalın**, *İtalik* ve ~~Üstü Çizili~~ formatlarını destekler.
 */
fun parseMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        // Regex: (**)kalın(**) veya (*)italik(*) veya (~~)çizili(~~)
        val regex = Regex("(\\*\\*.*?\\*\\*|\\*.*?\\*|~~.*?~~)")
        val matches = regex.findAll(text)

        for (match in matches) {
            // Eşleşme öncesi normal metni ekle
            append(text.substring(currentIndex, match.range.first))

            val matchedText = match.value
            when {
                matchedText.startsWith("**") && matchedText.endsWith("**") -> {
                    val innerText = matchedText.substring(2, matchedText.length - 2)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(innerText)
                    pop()
                }
                matchedText.startsWith("~~") && matchedText.endsWith("~~") -> {
                    val innerText = matchedText.substring(2, matchedText.length - 2)
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    append(innerText)
                    pop()
                }
                matchedText.startsWith("*") && matchedText.endsWith("*") -> {
                    val innerText = matchedText.substring(1, matchedText.length - 1)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(innerText)
                    pop()
                }
                else -> append(matchedText)
            }
            currentIndex = match.range.last + 1
        }
        
        // Kalan son kısmı ekle
        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
}
