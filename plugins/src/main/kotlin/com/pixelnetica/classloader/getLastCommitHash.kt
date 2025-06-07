import org.gradle.api.Project
import java.io.ByteArrayOutputStream
import java.io.File

fun Project.getLastCommitHash(subDir: String = ""): String {
    val stdout = ByteArrayOutputStream()

    project.exec {
        if (subDir.isNotEmpty()) {
            workingDir = File(subDir)
        }
        commandLine("git", "rev-parse", "--short", "HEAD")
        standardOutput = stdout
        isIgnoreExitValue = true
    }
    return stdout.toString().trim()
}

