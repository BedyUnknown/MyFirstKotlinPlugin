// IMPORTANTE: troque a linha "package" abaixo pela MESMA linha package
// que já existe no arquivo de exemplo do template. Mantenha também o nome da classe.
package com.github.yournamehere

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Environment
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.before
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Plugin de DIAGNÓSTICO: não altera nada, só registra em
 * /storage/emulated/0/Aliucord/audio_diag.txt as chamadas de áudio
 * que o Discord faz (modo de áudio, uso do stream, viva-voz, Bluetooth),
 * junto com quem chamou (stack trace).
 */
@AliucordPlugin
class MyFirstKotlinPlugin : Plugin() {

    private val logFile by lazy {
        File(Environment.getExternalStorageDirectory(), "Aliucord/audio_diag.txt")
    }
    private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private fun log(msg: String) {
        try {
            val trace = Throwable().stackTrace
                .filter {
                    !it.className.startsWith("com.aliucord") &&
                        !it.className.startsWith("top.canyie") &&
                        !it.className.startsWith("de.robv") &&
                        !it.className.startsWith("java.lang.Throwable")
                }
                .take(12)
                .joinToString("\n") { "    at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" }
            logFile.appendText("[${fmt.format(Date())}] $msg\n$trace\n\n")
        } catch (e: Throwable) {
            // ignora erros de escrita para nunca derrubar o app
        }
    }

    override fun start(context: Context) {
        try {
            logFile.parentFile?.mkdirs()
        } catch (e: Throwable) {
        }
        log("=== AudioDiag iniciado ===")

        // Modo de áudio: 0 = NORMAL (mídia), 3 = IN_COMMUNICATION (chamada)
        patcher.before<AudioManager>("setMode", Int::class.java) { param ->
            log("AudioManager.setMode(${param.args[0]})  [0=NORMAL, 3=IN_COMMUNICATION]")
        }

        patcher.before<AudioManager>("setSpeakerphoneOn", Boolean::class.java) { param ->
            log("AudioManager.setSpeakerphoneOn(${param.args[0]})")
        }

        patcher.before<AudioManager>("setBluetoothScoOn", Boolean::class.java) { param ->
            log("AudioManager.setBluetoothScoOn(${param.args[0]})")
        }

        patcher.before<AudioManager>("startBluetoothSco") {
            log("AudioManager.startBluetoothSco()")
        }

        // Uso do stream: 1 = MEDIA, 2 = VOICE_COMMUNICATION
        patcher.before<AudioAttributes.Builder>("setUsage", Int::class.java) { param ->
            log("AudioAttributes.Builder.setUsage(${param.args[0]})  [1=MEDIA, 2=VOICE_COMMUNICATION]")
        }

        patcher.before<AudioTrack.Builder>("setAudioAttributes", AudioAttributes::class.java) { param ->
            val a = param.args[0] as? AudioAttributes
            log("AudioTrack.Builder.setAudioAttributes(usage=${a?.usage}, contentType=${a?.contentType})")
        }
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}
