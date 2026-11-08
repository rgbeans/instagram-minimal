plugins { base }
val policyTests by tasks.registering(Exec::class) {
    commandLine("bash", "scripts/test-policy.sh")
}
tasks.named("check") { dependsOn(policyTests) }
tasks.named("assemble") { dependsOn(":patches:build") }
