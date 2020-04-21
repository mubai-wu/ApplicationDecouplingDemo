import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.TransformOutputProvider
import org.objectweb.asm.*

class AsmCrossCompileUtilsDump implements Opcodes {

    private static final String CLASS_PATH = 'com/wbh/decoupling/generate/'
    private static final String CLASS_SIMPLE_NAME = 'AsmCrossCompileUtils'
    private static final String CLASS_FULL_NAME = CLASS_PATH + CLASS_SIMPLE_NAME
    private static final String JAVA_FILE_NAME = CLASS_SIMPLE_NAME + '.java'
    private static final String CLASS_FILE_NAME = CLASS_SIMPLE_NAME + '.class'

    static void injectClass(TransformOutputProvider outputProvider, List<String> list) {
        File dstFile = outputProvider.getContentLocation(
                CLASS_SIMPLE_NAME,
                Collections.singleton(QualifiedContent.DefaultContentType.CLASSES),
                Collections.singleton(QualifiedContent.Scope.PROJECT),
                Format.DIRECTORY)
        byte[] bytes = dump(list)
        File file = new File(dstFile.absolutePath + File.separator + CLASS_PATH)
        file.mkdirs()

        FileOutputStream fos = new FileOutputStream(new File(file, CLASS_FILE_NAME))
        fos.write(bytes)
    }

    private static byte[] dump(List<String> list) throws Exception {

        ClassWriter cw = new ClassWriter(0)
        cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, CLASS_FULL_NAME, null, "java/lang/Object", null)
        cw.visitSource(JAVA_FILE_NAME, null)
        visitConstructionMethod(cw)
        visitInitMethod(cw, list)
        cw.visitEnd()

        return cw.toByteArray()
    }

    private static void visitConstructionMethod(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null)
        mv.visitCode()
        Label l0 = new Label()
        mv.visitLabel(l0)
        mv.visitLineNumber(3, l0)
        mv.visitVarInsn(ALOAD, 0)
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
        mv.visitInsn(RETURN)
        Label l1 = new Label()
        mv.visitLabel(l1)
        mv.visitLocalVariable("this", "Lcom/wbh/decoupling/generate/AsmCrossCompileUtils;", null, l0, l1, 0)
        mv.visitMaxs(1, 1)
        mv.visitEnd()
    }

    private static void visitInitMethod(ClassWriter cw, List<String> list) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "init", "()V", null, null)
        mv.visitCode()

        int lineNumber = 6

        for (int i = 0; i < list.size(); i++) {
            String it = list.get(i)

            Label l = new Label()
            mv.visitLabel(l)
            mv.visitLineNumber(lineNumber, l)
            String owner = it.substring(0, it.indexOf('.'))
            mv.visitMethodInsn(INVOKESTATIC, owner, "init", "()V", false)
            lineNumber++
        }

        Label l = new Label()
        mv.visitLabel(l)
        mv.visitLineNumber(lineNumber, l)
        mv.visitInsn(RETURN)
        mv.visitMaxs(0, 0)
        mv.visitEnd()
    }
}
