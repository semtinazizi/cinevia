plugins {
    // Sende yazan Android ve Kotlin versiyonları farklı olabilir, onlara dokunma.
    id("com.android.application") version "8.5.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" apply false

    // EKSİK OLAN SATIR BURASI: Bunu mutlaka eklemelisin!
    id("com.google.gms.google-services") version "4.4.1" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
}