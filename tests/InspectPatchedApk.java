import java.io.*;
import java.util.*;
import java.util.zip.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

/** Validate the delivered DEX files and the required injected hook families. */
public class InspectPatchedApk {
    public static void main(String[] args) throws Exception {
        Map<String, ClassDef> classes = new HashMap<>();
        int dexes=0, network=0, intents=0, fragments=0, captureRequests=0, captureReleases=0, chatCards=0;
        try (ZipFile zip=new ZipFile(args[0])) {
            for (ZipEntry entry:Collections.list(zip.entries())) {
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                dexes++;
                DexBackedDexFile dex=DexBackedDexFile.fromInputStream(Opcodes.getDefault(),new BufferedInputStream(zip.getInputStream(entry)));
                for (ClassDef cls:dex.getClasses()) if (classes.put(cls.getType(),cls)!=null) throw new AssertionError("Duplicate DEX class: "+cls.getType());
            }
            for (ClassDef cls:classes.values()) for (Method method:cls.getMethods()) {
                if (method.getImplementation()==null) continue;
                for (Instruction ins:method.getImplementation().getInstructions()) {
                    if (!(ins instanceof ReferenceInstruction)) continue;
                    Reference ref=((ReferenceInstruction)ins).getReference();
                    if (!(ref instanceof MethodReference)) continue;
                    MethodReference call=(MethodReference)ref;
                    if (!call.getDefiningClass().startsWith("Lcom/feurstagram/extension/")) continue;
                    if (cls.getType().startsWith("Lcom/feurstagram/")) continue;
                    ClassDef target=classes.get(call.getDefiningClass());
                    if (target==null) throw new AssertionError("Missing extension class: "+call);
                    boolean found=false;
                    for (Method candidate:target.getMethods()) if (candidate.getName().equals(call.getName()) && candidate.getParameterTypes().equals(call.getParameterTypes()) && candidate.getReturnType().equals(call.getReturnType())) {found=true;break;}
                    if (!found) throw new AssertionError("Missing extension method: "+call);
                    if (cls.getType().startsWith("Lcom/feurstagram/")) continue;
                    if (call.getDefiningClass().endsWith("/Block;") && call.getName().equals("throwIfBlocked")) network++;
                    if (call.getDefiningClass().endsWith("/NavigationGuard;") && call.getName().equals("filterIntent")) intents++;
                    if (call.getDefiningClass().endsWith("/ContentGuard;") && call.getName().equals("onFragmentViewCreated")) fragments++;
                    if (call.getDefiningClass().endsWith("/ScreenCapture;") && call.getName().equals("skipRequest")) captureRequests++;
                    if (call.getDefiningClass().endsWith("/ScreenCapture;") && call.getName().equals("skipRelease")) captureReleases++;
                    if (call.getDefiningClass().endsWith("/SharedMessages;") && call.getName().equals("onBind")) chatCards++;
                }
            }
        }
        if (network!=1 || intents<1 || fragments!=1) throw new AssertionError("Unexpected hooks: "+network+","+intents+","+fragments);
        if (captureRequests!=1 || captureReleases!=1 || chatCards<2) throw new AssertionError("Unexpected chat hooks: "+captureRequests+","+captureReleases+","+chatCards);
        System.out.println("PASS APK DEX structure: "+dexes+" DEX files, "+classes.size()+" unique classes, all injected extension calls resolve; hooks network="+network+", intents="+intents+", fragments="+fragments+", capture="+(captureRequests+captureReleases)+", chat cards="+chatCards);
    }
}
