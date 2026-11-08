package com.feurstagram.patches.shared

import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.NodeList
import org.w3c.dom.Node
import org.w3c.dom.Element

// Small local helpers keep the standalone build independent of GitHub Packages.
fun NodeList.asSequence(): Sequence<Node> = (0 until length).asSequence().map { item(it) }
fun NodeList.findElementByAttributeValue(name: String, value: String): Element? =
    asSequence().filterIsInstance<Element>().firstOrNull { it.getAttribute(name) == value }
fun Node.forEachChildElement(action: (Element) -> Unit) = childNodes.asSequence().filterIsInstance<Element>().forEach(action)
inline fun <reified T : Reference> Instruction.getReference(): T? = (this as? ReferenceInstruction)?.reference as? T
fun MutableClass.findMutableMethodOf(ref: MethodReference) = methods.single {
    it.name == ref.name && it.returnType == ref.returnType && it.parameterTypes == ref.parameterTypes
}
