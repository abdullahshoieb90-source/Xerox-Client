package com.origin.launcher.Launcher

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.util.Date

/** Best-effort diagnostics: logging must never prevent game startup. */
object LaunchTrace {
    private var files: List<File> = emptyList()
    @Volatile private var stage = "not started"
    @Volatile private var generation = 0

    @JvmStatic @Synchronized fun start(context: Context) {
        generation++
        files = listOfNotNull(
            context.getExternalFilesDir("launch_logs")?.let { File(it, "latest-launch.txt") },
            File(context.filesDir, "launch_logs/latest-launch.txt"),
            File(Environment.getExternalStorageDirectory(), "games/xelo_client/crash_logs/latest-launch.txt")
        )
        files.forEach { file ->
            try { file.parentFile?.mkdirs(); file.writeText("Launch started ${Date()}\n") }
            catch (e: Exception) { Log.w("LaunchTrace", "Cannot write ${file.absolutePath}", e) }
        }
        mark("Launch requested; Android ${android.os.Build.VERSION.RELEASE}; ABI ${android.os.Build.SUPPORTED_ABIS.joinToString()}")
        val currentGeneration = generation
        Thread({
            try { Thread.sleep(30000) } catch (_: InterruptedException) { return@Thread }
            if (generation == currentGeneration && stage != "MinecraftActivity resumed") {
                mark("30 second startup snapshot; last stage: $stage")
                Thread.getAllStackTraces().forEach { (thread, stack) ->
                    mark("Thread ${thread.name}:\n${stack.joinToString("\n")}")
                }
            }
        }, "LaunchDiagnostics").apply { isDaemon = true; start() }
    }

    @JvmStatic @Synchronized fun mark(message: String) {
        stage = message
        val line = "${Date()} [${Thread.currentThread().name}] $message\n"
        Log.i("LaunchTrace", message)
        files.forEach { try { it.appendText(line) } catch (_: Exception) { } }
    }
}
