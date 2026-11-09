package com.feurstagram.patches.ui

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.feurstagram.patches.shared.Constants.COMPATIBILITY_INSTAGRAM
import com.feurstagram.patches.shared.Constants.EXTENSION

private val secureRequest = Fingerprint(
    definingClass = "LX/08Vk;", name = "A01",
    parameters = listOf("Landroid/view/Window;", "Ljava/lang/String;"), returnType = "V",
)
private val secureRelease = Fingerprint(
    definingClass = "LX/08Vk;", name = "A02",
    parameters = listOf("Landroid/view/Window;", "Ljava/lang/String;"), returnType = "V",
)
private const val CAPTURE = "Lcom/feurstagram/extension/ScreenCapture;"

val chatScreenCapturePatch = bytecodePatch(
    name = "Chat screen capture",
    description = "Allows ordinary chat capture without suppressing protected media requests.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_INSTAGRAM)
    extendWith(EXTENSION)
    execute {
        secureRequest.method.apply {
            if ((accessFlags and 8) != 0 || (implementation?.registerCount ?: 0) < 4)
                throw PatchException("Unexpected secure-window manager request shape")
            addInstructionsWithLabels(0, """
                invoke-static/range { p1 .. p2 }, $CAPTURE->skipRequest(Landroid/view/Window;Ljava/lang/String;)Z
                move-result v0
                if-eqz v0, :apply_request
                return-void
                :apply_request
                nop
            """)
        }
        secureRelease.method.apply {
            if ((accessFlags and 8) != 0 || (implementation?.registerCount ?: 0) < 4)
                throw PatchException("Unexpected secure-window manager release shape")
            addInstructionsWithLabels(0, """
                invoke-static/range { p1 .. p2 }, $CAPTURE->skipRelease(Landroid/view/Window;Ljava/lang/String;)Z
                move-result v0
                if-eqz v0, :apply_release
                return-void
                :apply_release
                nop
            """)
        }
        println("Instagram Minimal: scoped two native secure-window hooks")
    }
}
