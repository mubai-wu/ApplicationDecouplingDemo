import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Status
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.api.transform.Format
import com.android.utils.FileUtils

import java.util.jar.JarFile

class WTransform extends Transform {

    private static final String TARGET = 'com/wbh/decoupling/generate/'

    private static List<String> sTargetList = new ArrayList<>()

    @Override
    String getName() {
        return 'WTransform'
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return Collections.singleton(QualifiedContent.DefaultContentType.CLASSES)
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        Set<? super QualifiedContent.Scope> set = new HashSet<>()
        set.add(QualifiedContent.Scope.PROJECT)
        set.add(QualifiedContent.Scope.SUB_PROJECTS)
        return set
    }

    @Override
    boolean isIncremental() {
        return true
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        sTargetList.clear()
        boolean isIncremental = transformInvocation.isIncremental()
//        println('isIncremental ==> ' + isIncremental)
        Collection<TransformInput> inputs = transformInvocation.getInputs()
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider()
        if (!isIncremental) {
            outputProvider.deleteAll()
        }
        inputs.each {
            it.getJarInputs().each { jarInput ->
                transformJar(jarInput, outputProvider, isIncremental)
            }

            it.getDirectoryInputs().each { dirInput ->
                transformDir(dirInput, outputProvider, isIncremental)
            }
        }
        injectClass(outputProvider)
    }

    private static void transformJar(JarInput jarInput, TransformOutputProvider outputProvider, boolean isIncremental) {
        File dstFile = outputProvider.getContentLocation(
                jarInput.getName(),
                jarInput.getContentTypes(),
                jarInput.getScopes(),
                Format.JAR)
        println('jar input name ==> ' + jarInput.name)
        println('jar input ==> ' + jarInput.file.absolutePath)
        println('dstFile ==> ' + dstFile.getAbsolutePath())

        JarFile jarFile = new JarFile(jarInput.file)
        println(jarFile.name)
        jarFile.entries().each {
            if (it.name.contains(TARGET)) {
                sTargetList.add(it.name)
                println(it.name)
            }
        }

        if (!isIncremental) {
            FileUtils.copyFile(jarInput.file, dstFile)
            return
        }

        Status status = jarInput.status
        switch (status) {
            case Status.NOTCHANGED:
                break
            case Status.ADDED:
            case Status.CHANGED:
                FileUtils.deleteIfExists(dstFile)
                FileUtils.copyFile(jarInput.file, dstFile)
                break
            case Status.REMOVED:
                FileUtils.deleteIfExists(dstFile)
                break
        }
    }

    private static void transformDir(DirectoryInput dirInput, TransformOutputProvider outputProvider, boolean isIncremental) {
        File dstDir = outputProvider.getContentLocation(
                dirInput.getName(),
                dirInput.getContentTypes(),
                dirInput.getScopes(),
                Format.DIRECTORY)
//        println('directory input ==> ' + dirInput.file.absolutePath)
//        println('dstDir ==> ' + dstDir.absolutePath)

//        printlnDir(dirInput.file)

        if (!isIncremental) {
            FileUtils.copyDirectory(dirInput.getFile(), dstDir)
            return
        }

        String srcDirPath = dirInput.getFile().getAbsolutePath()
        String dstDirPath = dstDir.getAbsolutePath()

        Map<File, Status> fileStatusMap = dirInput.getChangedFiles()
        fileStatusMap.entrySet().each { Map.Entry<File, Status> changedFileMapEntry ->

            Status status = changedFileMapEntry.getValue()
            File inputFile = changedFileMapEntry.getKey()
//            println('change file: ' + inputFile.getAbsolutePath() + ", status: " + status)
            String dstFilePath = inputFile.getAbsolutePath().replace(srcDirPath, dstDirPath)
            File dstFile = new File(dstFilePath)

            switch (status) {
                case Status.NOTCHANGED:
                    break
                case Status.REMOVED:
                    FileUtils.deleteIfExists(dstFile)
                    break
                case Status.ADDED:
                case Status.CHANGED:
                    FileUtils.deleteIfExists(dstFile)
                    FileUtils.copyFile(inputFile, dstFile)
                    break
            }
        }
    }

    private static void printlnDir(File srcFile) {
        srcFile.listFiles().each {
            if (it.directory) {
                printlnDir(it)
            } else {
                println(it.absolutePath)
            }
        }
    }

    private static void injectClass(TransformOutputProvider outputProvider) {
        AsmCrossCompileUtilsDump.injectClass(outputProvider, sTargetList)
    }
}