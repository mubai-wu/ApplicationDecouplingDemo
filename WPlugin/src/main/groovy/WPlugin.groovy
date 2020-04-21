import org.gradle.api.Plugin
import org.gradle.api.Project

class WPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        println('this is my WPlugin')
        def andr = project.extensions.getByName('android')
        andr.registerTransform(new WTransform())
    }
}