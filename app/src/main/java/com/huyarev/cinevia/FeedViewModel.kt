package com.huyarev.cinevia

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.huyarev.cinevia.network.Movie
import com.huyarev.cinevia.network.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class SocialPost(
    val id: String = "",
    val senderName: String = "",
    val senderEmail: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val movieTitle: String? = null,
    val moviePosterUrl: String? = null,
    val videoUrl: String? = null,
    val senderProfileImageUrl: String? = null,
    val movieId: Int? = null,
    val rating: Int? = null,
    @get:JvmName("getIsSpoiler") val isSpoiler: Boolean = false
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage
) : ViewModel() {

    private val _posts = mutableStateOf<List<SocialPost>>(emptyList())
    val posts: State<List<SocialPost>> = _posts

    private val _userPosts = mutableStateOf<List<SocialPost>>(emptyList())
    val userPosts: State<List<SocialPost>> = _userPosts

    private val _searchResults = mutableStateOf<List<Movie>>(emptyList())
    val searchResults: State<List<Movie>> = _searchResults

    private val _isUploading = mutableStateOf(false)
    val isUploading: State<Boolean> = _isUploading

    private val _currentUserProfileImage = mutableStateOf<String?>(null)
    val currentUserProfileImage: State<String?> = _currentUserProfileImage

    private val _dynamicUserImages = mutableStateOf<Map<String, String>>(emptyMap())
    val dynamicUserImages: State<Map<String, String>> = _dynamicUserImages

    private val API_KEY = BuildConfig.TMDB_API_KEY

    private val r2AccessKey = BuildConfig.R2_ACCESS_KEY
    private val r2SecretKey = BuildConfig.R2_SECRET_KEY
    private val r2Host = "50b81abf28d218b3b9858e9c69d4ae03.r2.cloudflarestorage.com"
    private val r2Endpoint = "https://$r2Host"

    private val workerDomain = "https://ancient-shape-df97.samatya231.workers.dev"

    private val bucketName = "cinevia-videos"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    init {
        listenForPosts()
        fetchUserProfileImage()
    }

    fun ensureUserExistsInDatabase() {
        val user = auth.currentUser ?: return
        val email = user.email?.lowercase() ?: return
        db.collection("users").document(email).set(mapOf("name" to (user.displayName ?: email.substringBefore("@"))), SetOptions.merge())
    }

    private fun fetchUserProfileImage() {
        val email = auth.currentUser?.email?.lowercase() ?: return
        db.collection("users").document(email).addSnapshotListener { snapshot, _ ->
            val rawUrl = snapshot?.getString("profileImageUrl")
            _currentUserProfileImage.value = getSignedUrl(rawUrl)
        }
    }

    private fun listenForPosts() {
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    _posts.value = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(SocialPost::class.java)?.copy(id = doc.id)
                    }
                }
            }
    }

    fun fetchUserPosts(email: String) {
        val normalizedEmail = email.lowercase()
        db.collection("posts")
            .whereEqualTo("senderEmail", normalizedEmail)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    _userPosts.value = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(SocialPost::class.java)?.copy(id = doc.id)
                    }
                }
            }
    }

    fun fetchDynamicProfileImage(email: String) {
        val normalizedEmail = email.lowercase().trim()
        if (normalizedEmail.isBlank()) return

        if (_dynamicUserImages.value.containsKey(normalizedEmail)) {
            val currentVal = _dynamicUserImages.value[normalizedEmail]
            if (currentVal == "pending" || (!currentVal.isNullOrBlank() && currentVal != "no_image")) return
        }

        _dynamicUserImages.value = _dynamicUserImages.value + (normalizedEmail to "pending")

        db.collection("users").document(normalizedEmail).get().addOnSuccessListener { doc ->
            val rawUrl = doc.getString("profileImageUrl")
            if (!rawUrl.isNullOrBlank()) {
                val safeUrl = getSignedUrl(rawUrl)!!
                _dynamicUserImages.value = _dynamicUserImages.value + (normalizedEmail to safeUrl)
            } else {
                _dynamicUserImages.value = _dynamicUserImages.value + (normalizedEmail to "no_image")
            }
        }.addOnFailureListener {
            _dynamicUserImages.value = _dynamicUserImages.value + (normalizedEmail to "no_image")
        }
    }

    fun deletePost(post: SocialPost, context: Context) {
        db.collection("posts").document(post.id).delete().addOnSuccessListener {
            Toast.makeText(context, "Gönderi silindi", Toast.LENGTH_SHORT).show()
        }
    }

    fun updatePostContent(postId: String, newContent: String, context: Context) {
        db.collection("posts").document(postId).update("content", newContent)
    }

    fun getSignedUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (url.contains("firebasestorage.googleapis.com")) return url

        if (url.contains("r2.dev") || url.contains("cloudflarestorage.com") || url.contains("workers.dev")) {
            val urlWithoutQuery = url.substringBefore("?")
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

    fun searchMovieForQuote(query: String) {
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.searchMovies(API_KEY, query)
                _searchResults.value = response.results ?: emptyList()
            } catch (e: Exception) { _searchResults.value = emptyList() }
        }
    }

    fun sendPost(
        content: String, 
        movieTitle: String? = null, 
        moviePosterUrl: String? = null, 
        videoUri: Uri? = null, 
        isSpoiler: Boolean = false, 
        movieId: Int? = null, 
        rating: Int? = null, 
        context: Context, 
        onComplete: () -> Unit
    ) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(context, "Oturum açık değil!", Toast.LENGTH_SHORT).show()
            return
        }

        val name = user.displayName ?: user.email?.substringBefore("@") ?: "Anonim"
        val email = user.email ?: ""
        val currentProfileImage = _currentUserProfileImage.value
        val newPost = SocialPost("", name, email, content, System.currentTimeMillis(), 0, emptyList(), movieTitle, moviePosterUrl, null, currentProfileImage, movieId, rating, isSpoiler)
        
        _isUploading.value = true

        if (videoUri != null) {
            uploadToR2(videoUri, context, false) { videoUrl ->
                val postWithVideo = newPost.copy(videoUrl = videoUrl)
                db.collection("posts").add(postWithVideo)
                .addOnSuccessListener {
                    Toast.makeText(context, "Gönderi paylaşıldı!", Toast.LENGTH_SHORT).show()
                    onComplete()
                    _isUploading.value = false
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Gönderi paylaşılamadı.", Toast.LENGTH_SHORT).show()
                    _isUploading.value = false
                }
            }
        } else {
            db.collection("posts").add(newPost)
                .addOnSuccessListener {
                    Toast.makeText(context, "Gönderi paylaşıldı!", Toast.LENGTH_SHORT).show()
                    onComplete()
                    _isUploading.value = false
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Gönderi paylaşılamadı.", Toast.LENGTH_SHORT).show()
                    _isUploading.value = false
                }
        }
    }

    private fun savePostToFirebase(name: String, email: String, content: String, movieTitle: String?, moviePosterUrl: String?, videoUrl: String?, isSpoiler: Boolean) {
        val currentProfileImage = _currentUserProfileImage.value
        val newPost = SocialPost("", name, email, content, System.currentTimeMillis(), 0, emptyList(), movieTitle, moviePosterUrl, videoUrl, currentProfileImage, null, null, isSpoiler)
        db.collection("posts").add(newPost)
        _isUploading.value = false
    }

    private val _moviePosts = mutableStateOf<List<SocialPost>>(emptyList())
    val moviePosts: State<List<SocialPost>> = _moviePosts

    fun fetchPostsByMovieId(movieId: Int) {
        db.collection("posts")
            .whereEqualTo("movieId", movieId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val postsList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(SocialPost::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.timestamp }
                    _moviePosts.value = postsList
                }
            }
    }

    // --- ÖZEL YAZIM S3 MOTORUMUZ (ASLA STREAMING YAPMAZ, ENGEL TANIMAZ) ---
    private fun uploadToR2(uri: Uri, context: Context, isProfileImage: Boolean, onSuccess: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val email = auth.currentUser?.email?.lowercase() ?: throw Exception("Kullanıcı bulunamadı")

                val fileName = if (isProfileImage) {
                    val safeEmail = email.replace("@", "_").replace(".", "_")
                    "profile_images_${safeEmail}_${UUID.randomUUID()}.jpg"
                } else {
                    "video_${UUID.randomUUID()}.mp4"
                }

                val mimeType = if (isProfileImage) "image/jpeg" else "video/mp4"

                performR2Upload(uri, context, fileName, mimeType) { publicUrl ->
                    withContext(Dispatchers.Main) {
                        if (isProfileImage) {
                            _isUploading.value = false
                            db.collection("users").document(email).set(mapOf("profileImageUrl" to publicUrl), SetOptions.merge())
                                .addOnSuccessListener {
                                    updateAllPostsProfileImage(email, publicUrl)
                                    _dynamicUserImages.value = _dynamicUserImages.value + (email to publicUrl)
                                    _currentUserProfileImage.value = publicUrl
                                    Toast.makeText(context, "Profil fotoğrafı güncellendi!", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            onSuccess(publicUrl)
                        }
                    }
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    _isUploading.value = false
                    Toast.makeText(context, "Yükleme Hatası: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun performR2Upload(uri: Uri, context: Context, fileName: String, mimeType: String, onSuccess: suspend (String) -> Unit) {
        var tempFile: File? = null
        try {
            tempFile = File(context.cacheDir, "upload_${UUID.randomUUID()}")
            context.contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(tempFile).use { output -> input.copyTo(output) } }

            if (tempFile.length() == 0L) throw Exception("Dosya okunamadı.")

            val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())

            // 1. ÖNCELİK: Cloudflare Worker üzerinden güvenli doğrudan yükleme (Secret key gerektirmez)
            val workerRequest = Request.Builder()
                .url("$workerDomain/$fileName")
                .put(requestBody)
                .addHeader("Content-Type", mimeType)
                .build()

            val workerResponse = try {
                okHttpClient.newCall(workerRequest).execute()
            } catch (e: Exception) {
                null
            }

            if (workerResponse != null && workerResponse.isSuccessful) {
                val publicUrl = "$workerDomain/$fileName"
                onSuccess(publicUrl)
                return
            }

            // 2. YEDEK (FALLBACK): İstemci taraflı AWS S3 İmzalı PUT İsteği
            val amzDate = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
            val dateOnly = amzDate.substring(0, 8)
            val region = "us-east-1"
            val service = "s3"

            val credentialScope = "$dateOnly/$region/$service/aws4_request"
            val hashedPayload = "UNSIGNED-PAYLOAD"

            val canonicalRequest = "PUT\n/$bucketName/$fileName\n\nhost:$r2Host\nx-amz-content-sha256:$hashedPayload\nx-amz-date:$amzDate\n\nhost;x-amz-content-sha256;x-amz-date\n$hashedPayload"

            val md = java.security.MessageDigest.getInstance("SHA-256")
            val canonicalHash = md.digest(canonicalRequest.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

            val stringToSign = "AWS4-HMAC-SHA256\n$amzDate\n$credentialScope\n$canonicalHash"
            val signature = calculateSignature(stringToSign, dateOnly, region, service)

            val authHeader = "AWS4-HMAC-SHA256 Credential=$r2AccessKey/$credentialScope, SignedHeaders=host;x-amz-content-sha256;x-amz-date, Signature=$signature"

            val fallbackRequest = Request.Builder()
                .url("$r2Endpoint/$bucketName/$fileName")
                .put(requestBody)
                .addHeader("Host", r2Host)
                .addHeader("x-amz-date", amzDate)
                .addHeader("x-amz-content-sha256", hashedPayload)
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", mimeType)
                .build()

            val response = okHttpClient.newCall(fallbackRequest).execute()

            if (!response.isSuccessful) {
                throw Exception("Cloudflare Yükleme Hatası: ${response.code} - ${response.body?.string()}")
            }

            val publicUrl = "$workerDomain/$fileName"
            onSuccess(publicUrl)
        } finally { tempFile?.let { if (it.exists()) it.delete() } }
    }

    private fun calculateSignature(stringToSign: String, dateStamp: String, regionName: String, serviceName: String): String {
        val kSecret = ("AWS4" + r2SecretKey).toByteArray(Charsets.UTF_8)
        val kDate = hmacSha256(dateStamp, kSecret)
        val kRegion = hmacSha256(regionName, kDate)
        val kService = hmacSha256(serviceName, kRegion)
        val kSigning = hmacSha256("aws4_request", kService)
        val signature = hmacSha256(stringToSign, kSigning)
        return signature.joinToString("") { "%02x".format(it) }
    }

    private fun hmacSha256(data: String, key: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    // --- NİHAİ KUSURSUZ PROFİL RESMİ YÜKLEME ---
    fun uploadProfileImage(uri: Uri, context: Context) {
        _isUploading.value = true
        uploadToR2(uri, context, true) {}
    }

    fun toggleLike(post: SocialPost) {
        val email = auth.currentUser?.email?.lowercase() ?: return
        val postRef = db.collection("posts").document(post.id)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val likedBy = snapshot.get("likedBy") as? List<String> ?: emptyList()
            if (likedBy.contains(email)) {
                transaction.update(postRef, "likedBy", FieldValue.arrayRemove(email))
                transaction.update(postRef, "likes", FieldValue.increment(-1))
            } else {
                transaction.update(postRef, "likedBy", FieldValue.arrayUnion(email))
                transaction.update(postRef, "likes", FieldValue.increment(1))
            }
            null
        }
    }

    private fun updateAllPostsProfileImage(email: String, imageUrl: String?) {
        val normalizedEmail = email.lowercase()
        db.collection("posts").whereEqualTo("senderEmail", normalizedEmail).get().addOnSuccessListener { snapshot ->
            for (doc in snapshot.documents) {
                db.collection("posts").document(doc.id).update("senderProfileImageUrl", imageUrl)
            }
        }
    }

    fun deleteProfileImage(context: Context) {
        val email = auth.currentUser?.email?.lowercase() ?: return
        db.collection("users").document(email).update("profileImageUrl", null).addOnSuccessListener {
            _currentUserProfileImage.value = null
            _dynamicUserImages.value = _dynamicUserImages.value + (email to "")
            updateAllPostsProfileImage(email, null)
            Toast.makeText(context, "Profil fotoğrafı silindi", Toast.LENGTH_SHORT).show()
        }
    }
}