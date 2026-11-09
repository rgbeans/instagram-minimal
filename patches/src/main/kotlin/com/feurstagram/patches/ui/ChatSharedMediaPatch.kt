package com.feurstagram.patches.ui

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.feurstagram.patches.shared.Constants.COMPATIBILITY_INSTAGRAM
import com.feurstagram.patches.shared.Constants.EXTENSION

// This pinned build binds every Direct card through the holder/payload pair.
// The bubble style carries the native message kind, including cached XMA shares.
private val bindParameters = listOf("LX/0Jrp;", "LX/0Kbr;")

val chatSharedMediaPatch = bytecodePatch(
    name = "Chat shared media filter",
    description = "Hides shared posts, Reels and Stories at the chat card binder.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_INSTAGRAM)
    extendWith(EXTENSION)
    execute {
        var count = 0
        var legacy = false
        classDefForEach classes@{ cls ->
            if (!cls.methods.any { it.name == "AX5" && it.parameterTypes == bindParameters && it.implementation != null }) return@classes
            mutableClassDefBy(cls).methods.filter { it.name == "AX5" && it.parameterTypes == bindParameters && it.returnType == "V" && it.implementation != null }.forEach { method ->
                val holder = if ((method.accessFlags and 8) == 0) 1 else 0
                method.addInstructions(0, "invoke-static/range { p$holder .. p${holder + 1} }, Lcom/feurstagram/extension/SharedMessages;->onBind(Ljava/lang/Object;Ljava/lang/Object;)V")
                count++
                legacy = legacy || cls.type == "LX/0OjB;"
            }
        }
        if (count < 2 || !legacy) throw PatchException("Direct card binders do not match this Instagram build")
        println("Instagram Minimal: filtered $count native chat card binders")
    }
}
