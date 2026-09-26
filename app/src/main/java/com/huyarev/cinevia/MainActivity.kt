package com.huyarev.cinevia

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.huyarev.cinevia.data.local.CineRevDatabase
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // --- 1. Android 13+ İzin İsteme Motoru ---
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startNotificationListener() // İzin verildiyse dinlemeye başla
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.e("MainActivity", "APP STARTED - VERSION TEST 7")
        
        // --- YENİ: ÇEVRİMDIŞI MOD (OFFLINE PERSISTENCE) ---
        val db = FirebaseFirestore.getInstance()
        db.firestoreSettings = firestoreSettings {
            isPersistenceEnabled = true
        }

        // Uygulama açılır açılmaz izni sor ve dinleyiciyi çalıştır
        askNotificationPermission()

        setContent {
            val auth = FirebaseAuth.getInstance()
            // Başlangıç durumunu belirliyoruz
            var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }

            // BÜYÜK SİHİR BURADA: Firebase'deki giriş/çıkış durumunu anlık olarak dinliyoruz!
            DisposableEffect(auth) {
                val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                    // Eğer kullanıcı çıkış yaparsa (currentUser null olursa) anında false olacak
                    val isUserPresent = firebaseAuth.currentUser != null
                    isLoggedIn = isUserPresent

                    // Kullanıcı giriş yaptıysa bildirim dinleyicisini tetikle
                    if (isUserPresent) {
                        startNotificationListener()
                    }
                }
                auth.addAuthStateListener(listener)
                onDispose {
                    auth.removeAuthStateListener(listener)
                }
            }

            // Yönlendirme
            if (isLoggedIn) {
                MainScreen() // İçerideyiz, afişleri ve menüyü göster
            } else {
                LoginScreen(
                    onLoginSuccess = { isLoggedIn = true } // Giriş yapınca içeri al
                )
            }
        }
    }

    // --- 2. İZİN KONTROLÜ ---
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                startNotificationListener()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // Android 12 ve altı zaten izin istemez, direkt başlat
            startNotificationListener()
        }
    }

    // --- 3. FİREBASE CANLI BİLDİRİM DİNLEYİCİSİ ---
    private fun startNotificationListener() {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val email = auth.currentUser?.email?.lowercase() ?: return

        // Sadece uygulama açıldıktan *sonra* gelen bildirimleri çalması için zaman damgası alıyoruz
        val appStartTime = System.currentTimeMillis()

        db.collection("notifications")
            .whereEqualTo("toEmail", email)
            .whereGreaterThan("timestamp", appStartTime) // Sadece yenileri getir!
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                if (snapshots == null) return@addSnapshotListener

                for (dc in snapshots.documentChanges) {
                    if (dc.type == DocumentChange.Type.ADDED) {
                        val fromName = dc.document.getString("fromName") ?: "Biri"
                        val message = dc.document.getString("message") ?: "sana bir bildirim gönderdi."
                        val type = dc.document.getString("type") ?: "notification"

                        // Bildirim tipine göre başlık belirliyoruz
                        val title = when (type) {
                            "message" -> "Yeni Mesaj: $fromName"
                            "follow" -> "Yeni Takipçi!"
                            "group_invite" -> "Grup Daveti"
                            else -> "CineRev Bildirimi"
                        }

                        // --- TELEFONUN SİSTEM BİLDİRİMİNİ TETİKLE! ---
                        NotificationHelper.showNotification(
                            context = this,
                            title = title,
                            message = "$fromName $message"
                        )
                    }
                }
            }
    }
}
