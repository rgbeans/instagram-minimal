package com.feurstagram.patches.ui

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.PatchException
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
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
        classDefForEach { cls ->
            if (!cls.type.startsWith("Lcom/feurstagram/")) cls.methods.forEach { method ->
                val code = method.implementation?.instructions?.toList() ?: return@forEach
                val sites = code.withIndex().filter { (index, insn) ->
                    val target = (insn as? ReferenceInstruction)?.reference as? MethodReference
                    target?.name == "getIntent" && target.parameterTypes.isEmpty()
                            && target.returnType == "Landroid/content/Intent;"
                            && code.getOrNull(index + 1)?.opcode == Opcode.MOVE_RESULT_OBJECT
                }
                if (sites.isNotEmpty()) {
                    val mutable = mutableClassDefBy(cls).methods.single { it.name == method.name && it.parameterTypes == method.parameterTypes && it.returnType == method.returnType }
                    sites.reversed().forEach { (index, _) ->
                        val register = (code[index + 1] as OneRegisterInstruction).registerA
                        mutable.addInstructions(index + 2, """
                            invoke-static/range { v$register .. v$register }, Lcom/feurstagram/extension/NavigationGuard;->filterIntent(Landroid/content/Intent;)Landroid/content/Intent;
                            move-result-object v$register
                        """)
                        guarded++
                    }
                }
            }
        }
        if (guarded == 0) throw PatchException("No incoming intent reads were guarded")
        println("Instagram Minimal: guarded $guarded intent reads")
    }
}
