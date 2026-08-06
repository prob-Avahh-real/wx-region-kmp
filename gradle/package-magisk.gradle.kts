import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.io.File

fun readVersionInfo(buildFilePath: String): Pair<String, String> {
    val buildFile = rootProject.file(buildFilePath)
    require(buildFile.exists()) { "Build file not found: ${buildFile.path}" }

    val content = buildFile.readText()
    val versionName = Regex("""versionName\s*=\s*\"([^\"]+)\"""").find(content)?.groupValues?.get(1)
        ?: error("Unable to find versionName in $buildFilePath")
    val versionCode = Regex("""versionCode\s*=\s*(\d+)""").find(content)?.groupValues?.get(1)
        ?: error("Unable to find versionCode in $buildFilePath")

    return versionName to versionCode
}

val (appVersion, appVersionCode) = readVersionInfo("composeApp/build.gradle.kts")

tasks.register("packageMagiskRelease") {
    group = "distribution"
    description = "Build release APKs and pack Magisk flashable zip"
    dependsOn(":composeApp:assembleRelease", ":lsposed:assembleRelease")

    val magiskDir = layout.projectDirectory.dir("magisk")
    val outDir = layout.buildDirectory.dir("dist")
    val outZip = outDir.map { it.file("WxRegionMask-magisk-v${appVersion}.zip") }

    inputs.dir(magiskDir)
    outputs.file(outZip)

    doLast {
        val maskApk = project(":composeApp").layout.buildDirectory
            .file("outputs/apk/release/composeApp-release.apk").get().asFile
        val lsposedApk = project(":lsposed").layout.buildDirectory
            .file("outputs/apk/release/lsposed-release.apk").get().asFile

        require(maskApk.exists()) { "Missing ${maskApk.path} — run assembleRelease first" }
        require(lsposedApk.exists()) { "Missing ${lsposedApk.path} — run assembleRelease first" }

        val zipFile = outZip.get().asFile
        zipFile.parentFile.mkdirs()

        val modulePropFile = File(magiskDir.asFile, "module.prop")
        if (modulePropFile.exists()) {
            val modulePropText = modulePropFile.readText()
            val updated = modulePropText
                .replace(Regex("^version=v.+$", RegexOption.MULTILINE), "version=v$appVersion")
                .replace(Regex("^versionCode=.+$", RegexOption.MULTILINE), "versionCode=$appVersionCode")
            modulePropFile.writeText(updated)
        }

        fun addFile(zos: ZipOutputStream, entryName: String, file: java.io.File) {
            zos.putNextEntry(ZipEntry(entryName))
            file.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
        }

        fun addDir(zos: ZipOutputStream, dir: java.io.File, prefix: String = "") {
            dir.listFiles()?.sortedBy { it.name }?.forEach { f ->
                val entry = if (prefix.isEmpty()) f.name else "$prefix/${f.name}"
                when {
                    f.isDirectory -> addDir(zos, f, entry)
                    f.name == ".gitkeep" -> Unit
                    f.parentFile?.name == "common" && f.extension == "apk" -> Unit
                    else -> addFile(zos, entry, f)
                }
            }
        }

        ZipOutputStream(zipFile.outputStream()).use { zos ->
            addDir(zos, magiskDir.asFile)
            addFile(zos, "common/wx-region-mask.apk", maskApk)
            addFile(zos, "common/wx-region-lsposed.apk", lsposedApk)
        }

        logger.lifecycle("Magisk zip: ${zipFile.absolutePath}")
        logger.lifecycle("  + common/wx-region-mask.apk (${maskApk.length() / 1024} KB)")
        logger.lifecycle("  + common/wx-region-lsposed.apk (${lsposedApk.length() / 1024} KB)")
    }
}

tasks.register("assembleReleaseAll") {
    group = "build"
    description = "Release APKs + Magisk zip"
    dependsOn("packageMagiskRelease")
}
