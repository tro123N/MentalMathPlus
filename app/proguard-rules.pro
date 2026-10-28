# Mental Math Plus – Regeln für R8 (Release-Build)

# LiteRT-LM (KI auf dem Handy): Der native Teil ruft Kotlin-Methoden über ihre Namen auf
# (z. B. SamplerConfig.getTopK, Rückrufe onMessage/onDone/onError). Nichts umbenennen oder entfernen.
-keep class com.google.ai.edge.litertlm.** { *; }
-keep interface com.google.ai.edge.litertlm.** { *; }
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# LiteRT-LM kann Werkzeuge auch per Kotlin-Reflection beschreiben; die App nutzt das nicht.
-dontwarn kotlin.reflect.full.**
-dontwarn kotlin.reflect.jvm.**

# Gson wird von LiteRT-LM für JSON genutzt.
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# Jetpack WindowManager (kommt über Bibliotheken mit): optionale Geräte-Erweiterungen fehlen absichtlich.
-dontwarn androidx.window.extensions.**
-dontwarn androidx.window.sidecar.**
