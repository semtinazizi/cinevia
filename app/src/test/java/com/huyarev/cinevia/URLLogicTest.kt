package com.huyarev.cinevia

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URLDecoder

class URLLogicTest {

    private val workerDomain = "https://ancient-shape-df97.samatya231.workers.dev"
    private val bucketName = "cinevia-videos"

    private fun getSignedUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (url.contains("firebasestorage.googleapis.com")) return url

        if (url.contains("r2.dev") || url.contains("cloudflarestorage.com") || url.contains("workers.dev")) {
            val urlWithoutQuery = url?.substringBefore("?") ?: return null
            val path = when {
                urlWithoutQuery.contains(".r2.dev/") -> urlWithoutQuery.substringAfter(".r2.dev/")
                urlWithoutQuery.contains(".workers.dev/") -> urlWithoutQuery.substringAfter(".workers.dev/")
                urlWithoutQuery.contains("/$bucketName/") -> urlWithoutQuery.substringAfter("/$bucketName/")
                else -> urlWithoutQuery.substringAfter(".com/")
            }
            return try {
                val decodedPath = URLDecoder.decode(path, "UTF-8")
                "$workerDomain/$decodedPath"
            } catch (e: Exception) {
                "$workerDomain/$path"
            }
        }

        return url
    }

    @Test
    fun testGetSignedUrl_R2Dev() {
        val input = "https://pub-649b2f16f93c4ed99b62ffc0d34f222c.r2.dev/profile_images_aaa_gmail_com_123.jpg"
        val expected = "$workerDomain/profile_images_aaa_gmail_com_123.jpg"
        assertEquals(expected, getSignedUrl(input))
    }

    @Test
    fun testGetSignedUrl_Firebase() {
        val input = "https://firebasestorage.googleapis.com/v0/b/project.appspot.com/o/image.jpg?alt=media"
        assertEquals(input, getSignedUrl(input))
    }

    @Test
    fun testGetSignedUrl_EncodedPath() {
        val input = "https://r2.dev/profile%20image.jpg"
        val expected = "$workerDomain/profile image.jpg"
        assertEquals(expected, getSignedUrl(input))
    }
}
