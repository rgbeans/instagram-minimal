package com.feurstagram.patches.ui

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.PatchException
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.feurstagram.patches.shared.Constants.COMPATIBILITY_INSTAGRAM
import com.feurstagram.patches.shared.Constants.EXTENSION

private object FragmentViewFingerprint : Fingerprint(
    definingClass = "Landroidx/fragment/app/Fragment;",
    name = "performViewCreated", parameters = emptyList(), returnType = "V",
)

val minimalSurfacesPatch = bytecodePatch(
    name = "Strict minimal surfaces",
    description = "Hides media fragments, profile grids and shared media cards before draw; restricts search rows to accounts and sanitizes deep links.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_INSTAGRAM)
    extendWith(EXTENSION)
    execute {
        FragmentViewFingerprint.method.apply {
            val returns = instructions.withIndex().filter { it.value.opcode == Opcode.RETURN_VOID }.map { it.index }
            if (returns.isEmpty()) throw PatchException("Fragment view dispatcher has no return")
            returns.reversed().forEach { index ->
                addInstructions(index, "invoke-static { p0 }, Lcom/feurstagram/extension/ContentGuard;->onFragmentViewCreated(Ljava/lang/Object;)V")
            }
        }
        var guarded = 0
        var outgoing = 0
        var web = 0
        var hot = 0
        classDefForEach classes@{ cls ->
            if (cls.type.startsWith("Lcom/feurstagram/")) return@classes
            cls.methods.forEach methods@{ method ->
                val code = method.implementation?.instructions?.toList() ?: return@methods
                val edits = mutableListOf<Pair<Int, String>>()
                if (method.name == "onNewIntent" && method.parameterTypes.firstOrNull() == "Landroid/content/Intent;"
                    && (method.accessFlags and 8) == 0) {
                    edits += 0 to "invoke-static/range { p1 .. p1 }, Lcom/feurstagram/extension/NavigationGuard;->filterIntent(Landroid/content/Intent;)Landroid/content/Intent;"
                    hot++
                }
                code.withIndex().forEach { (index, insn) ->
                    val target = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: return@forEach
                    if (target.name == "getIntent" && target.parameterTypes.isEmpty()
                        && target.returnType == "Landroid/content/Intent;" && code.getOrNull(index + 1)?.opcode == Opcode.MOVE_RESULT_OBJECT) {
                        val register = (code[index + 1] as OneRegisterInstruction).registerA
                        edits += (index + 2) to """
                            invoke-static/range { v$register .. v$register }, Lcom/feurstagram/extension/NavigationGuard;->filterIntent(Landroid/content/Intent;)Landroid/content/Intent;
                            move-result-object v$register
                        """
                        guarded++
                    }
                    val offset = if (insn.opcode == Opcode.INVOKE_STATIC || insn.opcode == Opcode.INVOKE_STATIC_RANGE) 0 else 1
                    val register = when (insn) {
                        is RegisterRangeInstruction -> insn.startRegister + offset
                        is FiveRegisterInstruction -> listOf(insn.registerC, insn.registerD, insn.registerE, insn.registerF, insn.registerG).getOrNull(offset)
                        else -> null
                    } ?: return@forEach
                    if (target.name in setOf("startActivity", "startActivityForResult", "startActivityIfNeeded", "startNextMatchingActivity")
                        && target.parameterTypes.firstOrNull() == "Landroid/content/Intent;") {
                        edits += index to "invoke-static/range { v$register .. v$register }, Lcom/feurstagram/extension/NavigationGuard;->filterIntent(Landroid/content/Intent;)Landroid/content/Intent;"
                        outgoing++
                    }
                    if (target.name in setOf("loadUrl", "postUrl") && target.parameterTypes.firstOrNull() == "Ljava/lang/String;") {
                        if (register > 255) throw PatchException("Web URL register cannot be rewritten safely")
                        edits += index to """
                            invoke-static/range { v$register .. v$register }, Lcom/feurstagram/extension/MinimalPolicy;->filterWebUrl(Ljava/lang/String;)Ljava/lang/String;
                            move-result-object v$register
                        """
                        web++
                    }
                }
                if (edits.isNotEmpty()) {
                    val mutable = mutableClassDefBy(cls).methods.single { it.name == method.name && it.parameterTypes == method.parameterTypes && it.returnType == method.returnType }
                    edits.sortedByDescending { it.first }.forEach { (index, text) -> mutable.addInstructions(index, text) }
                }
            }
        }
        if (guarded == 0 || outgoing == 0 || hot == 0 || web == 0) throw PatchException("Missing navigation guard sites: $guarded/$outgoing/$hot/$web")
        println("Instagram Minimal: guarded $guarded intent reads, $outgoing launches, $hot hot intents and $web web URLs")
    }
}
