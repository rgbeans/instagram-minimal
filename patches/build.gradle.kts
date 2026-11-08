plugins { base }

// The pinned public CLI supplies the exact patcher and dexlib used at runtime.
val compileBundle by tasks.registering(Exec::class) {
    workingDir(rootProject.projectDir)
    commandLine("bash", "scripts/compile-bundle.sh")
}
tasks.named("assemble") { dependsOn(compileBundle) }
