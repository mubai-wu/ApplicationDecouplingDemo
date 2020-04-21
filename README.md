# ApplicationDecouplingDemo
一个使用注解、Transform技术解耦的De'mo

## Demo 引入

假设当前有这么一种场景，我们有一个展示各种 Card 的应用，有只在国内展示的 CNACard、CNBCard 等，有只在国外展示的 ExpACard、ExpBCard 等，也有不管国内外都要展示的 ACard、BCard 等。  

### 简单的代码结构设计

首先设计出各个 Card，且都实现同一个接口 ICard:

```java
public interface ICard {
    String getCardName();
}

public class ACard implements ICard {
    @Override
    public String getCardName() {
        return "ACard";
    }
}

public class BCard implements ICard {
    @Override
    public String getCardName() {
        return "BCard";
    }
}

public class CNACard implements ICard {
    @Override
    public String getCardName() {
        return "CNACard";
    }
}

public class CNBCard implements ICard {
    @Override
    public String getCardName() {
        return "CNBCard";
    }
}

public class ExpACard implements ICard {
    @Override
    public String getCardName() {
        return "ExpACard";
    }
}

public class ExpBCard implements ICard {
    @Override
    public String getCardName() {
        return "ExpBCard";
    }
}
```
接着需要一个 Card 注册器，只有进行过注册的 Card 才会生效展示：
```java
class CardRegister {

    private List<ICard> mCardList;

    private CardRegister() {
        mCardList = new ArrayList<>();
    }

    static CardRegister getInstance() {
        return InternalHolder.sInstance;
    }

    private static class InternalHolder {
        final static CardRegister sInstance = new CardRegister();
    }

    void register(ICard card) {
        mCardList.add(card);
    }

    List<ICard> getCardList() {
        return mCardList;
    }
}
```
有了上述各类 Card 以及 Card 注册器后，我们需要设计一个 init() 方法，将需要展示的 Card 进行注册：
```java
public class CardManager {

    public static void init() {
        // 国内外都会展示的 Card
        CardRegister.getInstance().register(new ACard());
        CardRegister.getInstance().register(new BCard());

        if (Feature.sIsCN) { // 只在国内展示的 Card
            CardRegister.getInstance().register(new CNACard());
            CardRegister.getInstance().register(new CNBCard());
        } else { // 只在国外展示的 Card
            CardRegister.getInstance().register(new ExpACard());
            CardRegister.getInstance().register(new ExpBCard());
        }
    }

    public static List<ICard> getAllCard() {
        return CardRegister.getInstance().getCardList();
    }
}
```
通过 Feature 来控制注册国内的卡片还是国外的卡片，然后我们需要在 Application 中调用 CardManager.init() 方法，使我们的注册过程生效：
```java
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CardManager.init();
    }
}
```

最后我们在 Activity 里展示出我们注册的 Card 列表：
```java
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                return new RecyclerView.ViewHolder(new TextView(MainActivity.this)) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                ((TextView) viewHolder.itemView).setText(CardManager.getAllCard().get(i).getCardName());
            }

            @Override
            public int getItemCount() {
                return CardManager.getAllCard().size();
            }
        });
    }
}
```

展示结果如下：  
![](https://note.youdao.com/yws/api/personal/file/WEBf30b15328a3c9ca41110a6d420f008fa?method=download&shareKey=de3c79d5e70d6fa7d94575a6bc77c41e)

代码的目录结构如下：   
![](https://note.youdao.com/yws/api/personal/file/WEBaa936929697ba1bc47eab329c41a5fc8?method=download&shareKey=4205825ee96d1e76a89df3aa3257dbb0)

### 小结
上述的代码结构有一个明显缺点，就是打包出来的 apk 包含了所有的 Card 以及资源。  

## 代码结构改造

我们重构下代码结构：

1. 我们先创建一个业务层 module，名为 BusinessLayer，将公共相关业务的代码迁移到此 module 下，也就是 ICard、ACard、BCard、CardRegister 还有 Activity，并重新创建一个新的 CardManager，只将 ACard 和 BCard 进行注册：
```java
public class CardManager {

    public static void init() {
        registerCard(new ACard());
        registerCard(new BCard());
    }

    public static void registerCard(ICard card) {
        CardRegister.getInstance().register(card);
    }

    public static List<ICard> getAllCard() {
        return CardRegister.getInstance().getCardList();
    }
}
```

另外需要在 LayerApplication 中调用 CardManager.init() 方法。

BusinessLayer 的代码结构如下：   
![t](https://note.youdao.com/yws/api/personal/file/WEBb45f9956d3ef42265253c4d2570857f4?method=download&shareKey=e475268219ff939ff1d08b370a830354)

2. 然后分别为国内业务和海外业务创建 module，分别命名为 BusinessCN、BusinessExp，两个 module 都需要在 build.gradle 里依赖 BusinessLayer 这个 公共 module， 
```
// build.gradle
dependencies {
    // ... ...
    implementation project(path: ':businesslayer')
}
```
我们将相关业务代码迁移到各自 module 下，同样也需要分别在两个 module 创建新的 CardManager 进行 Card 的注册和在 Application 调用 CardManager.init()  
```java
// module BusinessCN
public class CnCardManager {

    public static void init() {
        CardManager.registerCard(new CNACard());
        CardManager.registerCard(new CNBCard());
    }
}
```

```java
// module BusinessExp
public class ExpCardManager {

    public static void init() {
        CardManager.registerCard(new ExpACard());
        CardManager.registerCard(new ExpBCard());
    }
}
```

这里关于 Application 需要说明下，因为这里使用了多个 Application 会在编译的时候导致冲突，需要特别处理下，以 BusinessCn 为例说明：  
BusinessLayer 中注册的 LayerApplication 和 BusinessCn 中注册的 CnApplication 发生冲突，需要在 BusinessCn 的 AndroidManifest.xml 中使用 tools:replace 声明使用 CnApplication 替代 LayerApplication。  
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    package="com.wbh.decoupling.businesscn" >

    <application
        android:name=".CnApplication"
        tools:replace="android:name">
    </application>

</manifest>
```
因为 LayerApplication 被替换了，所以 CnApplication 和 ExpApplication 都需要继承 LayerApplication，这样才能保证 LayerApplication 中的代码会执行。   


两个 module 的代码结构如下:  
![](https://note.youdao.com/yws/api/personal/file/WEB39ee0be072f136e5f0a60f19dc9d9e56?method=download&shareKey=5d887af850847e3b40517764b3c8069d)

![](https://note.youdao.com/yws/api/personal/file/WEB8b20862135661acd989d44f08e2d6c49?method=download&shareKey=ec428172853e6957a9034014d32b0d40)

3. 最后在主项目中，src 中的代码全部都迁移到各自对应的 module 里了，所以 src 中是没有代码的，但是在 build.gradle 里，我们通过渠道打包的方式，针对国内和国外两种情况各自打出不同的 apk 包，build.gradle 的配置如下：  
```
android {
    // ... ...
    
    flavorDimensions 'test'
    productFlavors {
        cn {
        }

        exp {
        }
    }
}

dependencies {
    // ... ...

    cnImplementation project(path: ':businesscn')
    expImplementation project(path: ':businessexp')
}
```

通过 gradlew assembleCnRelease 和 gradlew assembleExpRelease 就能打包出不同的 apk 包，且不会出现 cn 的 apk 包中有 exp 的 Card 代码或者 exp 的 apk 包中有 cn 的 Card 代码。  

### 小结
小结下上述重构后的代码结构。 

![](https://note.youdao.com/yws/api/personal/file/WEB78cec4a89b6b7e8cc9a36108e26e5f98?method=download&shareKey=4ec38360d08e6de3ca6a381b40f94a60)

- module BusinessLayer 中包含了公共的所有业务代码;    
- modlue BusinessCn 中只包含国内的业务代码，并且依赖 BusinessLayer;  
- module BusinessExp 中只包含国外的业务代码，并且依赖 BusinessLayer;  
- 主项目 app 中，通过渠道打包的方式，cn 渠道依赖 BusinessCn, exp 渠道依赖 BussinessExp。  


到这里，其实代码已经做了很好的隔离，但这毕竟只是 Demo，场景比较简单，而实际工作中的业务场景往往比 Demo 会复杂得多。

## 进一步代码结构改造

仔细观察每个 module, 都有一个 `XXCardManager` 类，都有一个 `init()` 方法，方法中都是 `CardManager.registerCard(...)`的形式来注册每一个 Card。从这个角度来改造，我们使用 APT + javapoet 的方式来自动生成这些代码。  

一. 首先，我们先整理下思路，明确好要生成的代码是怎样的：
1. 为了方便后续的管理（看到后面的内容就明白了），对于生成的代码，统一放在 `com.wbh.decoupling.generate` 包下；
2. 多个 module，所以会生成多个 CardManager，所以需要在类名后加个后缀以作区分；

```java
// com.wbh.decoupling.generate

public class CardManager_??? {

    public static void init() {
        CardManager.registerCard(new ???Card());
        CardManager.registerCard(new ???Card());
    }
}
```

二. 创建一个 java module，名为 annotation，用来存放注解相关的代码。  
创建一个 Register 注解：  
```java
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Register {
}
```

![](https://note.youdao.com/yws/api/personal/file/WEB458ed0f7374f7edb143a0453188406f1?method=download&shareKey=1a8646890368211a823371d6a067c1e5)

三. 创建一个 Java module，名为 compile，用来存放注解处理相关代码。  
1. build.gradle 中引入以下三个依赖  
```
dependencies {
    implementation project(path: ":annotation")
    implementation 'com.google.auto.service:auto-service:1.0-rc3'
    annotationProcessor 'com.google.auto.service:auto-service:1.0-rc4'
    implementation 'com.squareup:javapoet:1.8.0'
}
```
- auto-service 是为了更方便的注册 processor  
- javapoet 是为了更方便的生成代码

2. 创建注解处理器，并且注册此处理器  
```java
@AutoService(Processor.class)
public class RegisterProcessor extends AbstractProcessor{

    private Map<String, TypeElement> mMap = new HashMap<>();
    private ProcessingEnvironment mProcessingEnvironment;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mProcessingEnvironment = processingEnvironment;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(Register.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {

        for (Element element : roundEnvironment.getElementsAnnotatedWith(Register.class)) {
            processElement(element);
        }

        if (roundEnvironment.processingOver()) {
            generateCode();
        }
        return true;
    }

    private void processElement(Element element) {
        TypeElement typeElement = (TypeElement) element;
        String qualifiedName = typeElement.getQualifiedName().toString();
        if (mMap.get(qualifiedName) == null) {
            mMap.put(qualifiedName, typeElement);
        }
    }

    private void generateCode() {
        if (mMap.isEmpty()) return;

        Set<TypeElement> set = new HashSet<>();
        set.addAll(mMap.values());

        GenerateClassHelper helper = new GenerateClassHelper(mProcessingEnvironment, set);
        helper.generateCode();
    }

}
```
- 使用 @AutoService 的方式来自动注册此处理器，会在 build/classes/java/main 下生成 META-INF/services/javax.annotation.processing.Processor 文件：  
![](https://note.youdao.com/yws/api/personal/file/WEB46c8c49bbb0d22c9c51a698e8f9a7d34?method=download&shareKey=1bbdf20f110a6c17588a1ca0a2733b89) 

打开此文件内容如下:  
```java
com.wbh.decoupling.compile.RegisterProcessor
```
如果不使用注解方式注册，就需要自己手动实现 META-INF/services/javax.annotation.processing.Processor 内容。  
- process() 方法中收集所有被 Register 注解的 Element，然后通过 GenerateClassHelper 类来生成代码。因为 process() 方法会执行多次，所以使用 roundEnvironment.processingOver() 判断只有在最后的时候再去生成代码。    

3. 我们来实现 GenerateClassHelper 的代码：  
```java
public class GenerateClassHelper {

    private static final String PACKAGE_NAME = "com.wbh.decoupling.generate";
    private static final String CLASS_NAME_PREFIX = "CardManager_";
    private static final String METHOD_NAME = "init";
    private static final ClassName CARD_MANAGER = ClassName.get("com.wbh.decoupling.businesslayer.card", "CardManager");

    private Filer mFiler;
    private Elements mElementUtils;

    private Set<TypeElement> mElementSet;

    public GenerateClassHelper(ProcessingEnvironment processingEnvironment, Set<TypeElement> set) {
        mFiler = processingEnvironment.getFiler();
        mElementUtils = processingEnvironment.getElementUtils();
        mElementSet = set;
    }


    public void generateCode() {
        try {
            JavaFile javaFile = JavaFile.builder(PACKAGE_NAME, getGenTypeSpec()).build();
            javaFile.writeTo(mFiler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private TypeSpec getGenTypeSpec() {
        return TypeSpec.classBuilder(getClassName())
                .addModifiers(Modifier.PUBLIC)
                .addMethod(getGenInitMethodSpec())
                .build();
    }

    private String getClassName() {
        for (TypeElement element : mElementSet) {
            return CLASS_NAME_PREFIX + EncryptHelper.md5String(mElementUtils.getPackageOf(element).getQualifiedName().toString());
        }
        return "";
    }

    private MethodSpec getGenInitMethodSpec() {
        String format = "$T.registerCard(new $T())";
        CodeBlock.Builder builder = CodeBlock.builder();
        for (TypeElement typeElement : mElementSet) {
            ClassName className = ClassName.get(typeElement);
            builder.addStatement(format, CARD_MANAGER, className);
        }

        CodeBlock codeBlock = builder.build();
        return MethodSpec.methodBuilder(METHOD_NAME)
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.STATIC)
                .addCode(codeBlock)
                .build();
    }

}
```
GenerateClassHelper 使用 JavaPoet 库来生成代码，JavaFile、TypeSpec、MethodSpec、CodeBlock 等，都是 JavaPoet 的，代码里按照我们想要生成的 CardManager_??? 类来编写。  

![](https://note.youdao.com/yws/api/personal/file/WEBebf2898300719f72fe6aca32849c4abb?method=download&shareKey=ac13313934a087dad9eead1276309fcc)

四. 在 BusinessLayer、BusinessCn、BusinessExp 中的各个 build.gradle 中都依赖 annotation 和 compile 两个 module   
```
dependencies {
    implementation project(path: ':annotation')
    annotationProcessor project(path: ':compile')
}
```
然后对需要注册的 Card 类加上 @Register 的注解，比如：  
```java
@Register
public class ACard implements ICard {
}
```
最后我们运行下代码，就能自动生成 CardManager 相关的代码，它在 build/generated/source/apt 目录下。  

我们来看下 BusinessLayer 的目录：  
![](https://note.youdao.com/yws/api/personal/file/WEB56b61764dfd86ddec6a12443af28d03b?method=download&shareKey=d4d0da847b63643995d45185f197aa43)  

打开 CardManager_becf3fc7606c9b461025f1def7ff27ac 文件：  
```java
package com.wbh.decoupling.generate;

import com.wbh.decoupling.businesslayer.card.ACard;
import com.wbh.decoupling.businesslayer.card.BCard;
import com.wbh.decoupling.businesslayer.card.CardRegister;

public class CardManager_becf3fc7606c9b461025f1def7ff27ac {
  public static void init() {
    CardManager.registerCard(new ACard());
    CardManager.registerCard(new BCard());
  }
}
```
其他 module 中也会生成类似的此文件，这里就不一一展示了。  

既然 CardManager 已经是通过 APT 自动生成了，那么我们手写的各个 module 中的 CardManager 类就可以删掉了。  

## 调用注解生成的各个类文件

那么问题来了，CardManager_??? 这几个文件自动生成后，该怎么调用呢？这里主要说明两种方式。  

### 遍历 dex 文件的方式
这种方式主要是通过遍历 dex 文件中所有以`com.wbh.decoupling.generate`开头的类，这个也是之前说到要将所有生成的类都放在这个包下的原因，然后反射的方式来调用这些类的 init() 方法。  

在 BusinessLayer 里创建 CrossCompileUtils 类来完成这些操作，然后在 Application 中调用 CrossCompileUtils.init() 方法执行此过程，看下代码的实现：  

```java
public class CrossCompileUtils {

    private static final String GENERATE_CODE_PACKAGE_NAME = "com.wbh.decoupling.generate";
    private static final String METHOD_NAME = "init";

    public static void init(Context context) {
        try {
            List<String> targetClassList = getTargetClassList(context, GENERATE_CODE_PACKAGE_NAME);
            for (String className : targetClassList) {
                Class<?> cls = Class.forName(className);
                Method method = cls.getMethod(METHOD_NAME);
                method.invoke(null);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    // 获取以 target 为开头的所有类
    private static List<String> getTargetClassList(Context context, String target) {
        List<String> classList = new ArrayList<>();

        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
            String path = info.sourceDir;
            DexFile dexFile = new DexFile(path);

            Enumeration<String> entries = dexFile.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement();
                if (name.startsWith(target)) {
                    classList.add(name);
                }
            }

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return classList;
    }
}
```
这种方式有个很明显的缺陷，就是整个过程是在运行时进行的，当代码量越多，此过程越耗时。


### gradle plugin + transform 的方式

这种方式主要通过在编译期插入相应字节码的方式，将耗时放在编译期以优化运行时效率。  

我们先整理接下来的主要任务，在编译期生成类`AsmCrossCompileUtils`，这个类的有个 init() 方法，这个方法会调用所有 CardManager_XXX.init():  
```java
package com.wbh.decoupling.generate;

public class AsmCrossCompileUtils {

    public static void init() {
        CardManager_becf3fc7606c9b461025f1def7ff27ac.init();
        CardManager_dc2db21188334cfca97494d99700395.init();
    }
}
```

假定以上的类已经生成了，那么 CrossCompileUtils 可以改为：  
```java
public class CrossCompileUtils {
    public static void init(Context context) {
        initByAsm();
    }

    private static void initByAsm() {
        try {
            Class cls = Class.forName("com.wbh.decoupling.generate.AsmCrossCompileUtils");
            Method method = cls.getMethod(METHOD_NAME);
            method.invoke(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

这种方式只需要通过一次反射，相比遍历 dex 的方式，性能就好很多了。  

这种方式有两个关键点：一个是如何获取注解生成的各个类，一个是如何生成 AsmCrossCompileUtils 类。  


#### 自定义 gradle plugin
我们创建一个 module（哪一种类型都可以） ，姑且命名为 WPlugin 作为插件名字，然后将 src 目录下的所有文件都删除，只保留 main 目录，main 目录下的文件也全部删除，然后在 main 目录下创建 groovy 目录（这是因为 gradle 的语法是 groovy），我们编写的插件代码就在这目录下。

接下来我们需要配置下 build.gradle，将原本的 build.gradle 内容都删除，配置如下：  

```groovy
apply plugin: 'groovy'  // 因为plugin是由groovy语法编写，所以需要应用此插件

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation gradleApi() // 自定义插件中需要用到 gradle 的各种 api
}

sourceCompatibility = "1.7"
targetCompatibility = "1.7"

group = 'com.wbh.decoupling.plugin'  // 自定义插件的组别
version = '1.0.0'  // 自定义插件的版本号

```

然后我们在 groovy 目录下创建一个 groovy 文件，命名为 WPlugin： 
```groovy
import org.gradle.api.Plugin
import org.gradle.api.Project

class WPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        println('this is my WPlugin')
    }
}
```
插件代码执行的入口就是这里的 apply() 方法，不过我们还得需要注册这个插件。在 main 目录下创建 resources 目录，在该目录下创建 META-INF.gradle-plugins 目录，然后在该目录下创建 xxx.properties 文件，这里的 xxx 表示当我们在其他项目应用此插件时的名字 apply plugin: 'xxx'，我们还是将之命名为 WPlugin，在这个文件里编写内容如下：
```
implementation-class=WPlugin
```
`implementation-class` 用来配置插件入口类的，这里也就是配置我们自定义的 WPlugin 类。


至此整个自定义插件的结构已经完成了，在看下这个目录结构如下：

![](https://note.youdao.com/yws/api/personal/file/WEB314e4aa1d89a2dbe97e8fd14110193fe?method=download&shareKey=92fc01f441d4b3e31a3a2d9f0f576c35)


#### 发布 plugin

在使用自定义的插件前，得先发布这个插件。  

这里我们将插件发布到本地目录下，在根目录下创建 maven 目录，用来存放我们发布的插件，然后在 build.gradle 配置如下： 
```groovy
apply plugin: 'maven'

uploadArchives {
    repositories {
        mavenDeployer {
            repository(url: uri('../maven'))  // 指定发布到的目录
        }
    }
}
```

然后点击以下 uploadArchives ，运行发布插件。  

![](https://note.youdao.com/yws/api/personal/file/WEBb8d648e3451012d4d781b45b9c84b981?method=download&shareKey=022e77692449656ba968657b0e7294d3)


接下来我们就能在 maven 目录下看到我们发布的插件了。  

![](https://note.youdao.com/yws/api/personal/file/WEB54e3272703745ce3457702a138734ec2?method=download&shareKey=5e2008a2230f98e48fc005f9ca3673d0)

#### 使用自定义的插件
在 app 这个 module 中使用自定义的插件 WPlugin，在 build.gradle 中配置如下：  
```groovy
apply plugin: WPlugin

buildscript {
    repositories {
        maven {
            url uri('../maven')
        }
    }
    dependencies {
        classpath 'com.wbh.decoupling.plugin:WPlugin:1.0.0'
    }
}
```
然后 Sync Gradle，我们就能在 Build 窗口中看到 WPlugin 打印的内容。  
![](https://note.youdao.com/yws/api/personal/file/WEB1cd96819543a8f61c0af7ec68afcd2e6?method=download&shareKey=279d37ea533d8cb1fb052cd8e8298c4f)


至此，我们说完了如何自定义、发布以及使用 gradle 插件。

接下来我们该完善下这个 WPlugin 的内容。  

#### 关于 Transform
Apk 的代码编译过程有： .java ---> .class ---> .dex  

注解执行是在.java 编译为 .class 这一过程，而 .class 转化为 .dex 过程，是经过一系列的 transform 链完成的，比如 proguard、jarMerger、multi-dex 等，都是这条 transform 链中的一环，每一个 tansform 节点都有个输入和输出，该节点的输出会作为下一个 transform 节点的输入。

我们可以自己实现 一个Transform，插入这条 Transform 链中，以实现对字节码的操作。

#### 自定义 Transform
编写 Transform，需要使用到 gradle 的 API，所以我们需要在 build.gradle 添加如下的依赖：  
```groovy
implementation'com.android.tools.build:gradle-api:3.4.2'
```
注意：由于 gradle 插件使用了上述依赖，所以在使用该插件的项目的build.grdle里需要配置如下：
```java
buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:3.4.2'
    }
}
```

接着创建一个类继承 Transform ：  
```groovy
class WTransform extends Transform {

    @Override
    String getName() {
        return null
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return null
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return null
    }

    @Override
    boolean isIncremental() {
        return false
    }
    
    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
    }
}
```
- getName(): 返回自定义的 Transform 名字  
- geInputTypes(): 指定 Transform 要处理的输入类型，主要有 `QualifiedContent.DefaultContentType.CLASSES` 和 `QualifiedContent.DefaultContentType.RESOURCES` 两种类型，对应为 .class 文件 和 java 资源文件

- getScopes():指定输入文件的所属的范围。
```java
public interface QualifiedContent {
    enum Scope implements ScopeType {
        /** Only the project (module) content */
        PROJECT(0x01),
        /** Only the sub-projects (other modules) */
        SUB_PROJECTS(0x04),
        /** Only the external libraries */
        EXTERNAL_LIBRARIES(0x10),
        /** Code that is being tested by the current variant, including dependencies */
        TESTED_CODE(0x20),
        /** Local or remote dependencies that are provided-only */
        PROVIDED_ONLY(0x40),
    }
}    
```
- isIncremental():当前是支持增量编译
- transform():执行转化的方法，通过参数 transformInvocation 可以获取到该节点的输入和输出:  
```groovy
Collection<TransformInput> inputs = transformInvocation.getInputs()
TransformOutputProvider outputProvider = transformInvocation.getOutputProvider()
```
TransformInput 分为两类，一类是 Jarinput,`TransformInput#getJarInputs()`，一类是 DirectoryInput,`TransformInput#getDirectoryInputs()`。而 TransformOutputProvider 指向了文件/目录输出路径。


#### 注册 Transform
要使我们自定义的 Transform 生效参与到编译过程中，还需要注册 WTransform，注册过程很简单，只要在 Plugin 中注册即可。  

```groovy
class WPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        println('this is my WPlugin')
        def andr = project.extensions.getByName('android')
        andr.registerTransform(new WTransform())
    }
}
```

#### 完善 Transform
```java
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.api.transform.Format
import com.android.utils.FileUtils

class WTransform extends Transform {

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
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        Collection<TransformInput> inputs = transformInvocation.getInputs()
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider()
        inputs.each {
            it.getJarInputs().each { jarInput ->
                transformJar(jarInput, outputProvider)
            }

            it.getDirectoryInputs().each { dirInput ->
                transformDir(dirInput, outputProvider)
            }
        }
    }

    private static void transformJar(JarInput jarInput, TransformOutputProvider outputProvider) {
        File dstFile = outputProvider.getContentLocation(
                jarInput.getName(),
                jarInput.getContentTypes(),
                jarInput.getScopes(),
                Format.JAR)
        FileUtils.copyFile(jarInput.getFile(), dstFile)
        println('jarInputFile ==> ' + jarInput.file.absolutePath)
        println('dstFile ==> ' + dstFile.absolutePath)
    }

    private static void transformDir(DirectoryInput dirInput, TransformOutputProvider outputProvider) {
        File dstDir = outputProvider.getContentLocation(
                dirInput.getName(),
                dirInput.getContentTypes(),
                dirInput.getScopes(),
                Format.DIRECTORY)
        FileUtils.copyDirectory(dirInput.getFile(), dstDir)
        println('directory input ==> ' + dirInput.file.absolutePath)
        println('dstDir ==> ' + dstDir.absolutePath)
    }
}
```
transform() 方法做的事很简单，就是获取所有的输入文件/目录，然后拷贝到输出路径。   

我们重新 uploadArchives 这个插件，然后 clear project，在重新构建项目，就能在 build 窗口看到我们打印的输出：  
![](https://note.youdao.com/yws/api/personal/file/WEB5eb6ec091c6c20b6c735cf86902d10e3?method=download&shareKey=4f23235354d454c97432faee488e9ee5)  

根据打印的输入，我们能在相应的目录下找到 WTransform 生成的文件：   
![](https://note.youdao.com/yws/api/personal/file/WEB491982f0706c16494368b96171edf8a9?method=download&shareKey=087930a4fbba985629893d2d269df1c1)

由于每次编译所有文件/目录都会重新复制一次，所以加上支持增量编译：  
```java
class WTransform extends Transform {

    @Override
    boolean isIncremental() {
        return true
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        boolean isIncremental = transformInvocation.isIncremental()
        println('isIncremental ==> ' + isIncremental)
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
    }

    private static void transformJar(JarInput jarInput, TransformOutputProvider outputProvider, boolean isIncremental) {
        File dstFile = outputProvider.getContentLocation(
                jarInput.getName(),
                jarInput.getContentTypes(),
                jarInput.getScopes(),
                Format.JAR)
        println('jar input ==> ' + jarInput.file.absolutePath)
        println('dstFile ==> ' + dstFile.getAbsolutePath())

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
        println('directory input ==> ' + dirInput.file.absolutePath)
        println('dstDir ==> ' + dstDir.absolutePath)

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
            println('change file: ' + inputFile.getAbsolutePath() + ", status: " + status)
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
}
```
增量编译的文件都有个状态 Status，根据文件的状态做相应不同对应操作即可。
- Status.NOTCHANGED：该文件没有变动，所以不需要重新复制一份
- Status.REMOVED：该文件被删除，所以对应输出文件也要删除
- Status.ADDED：该文件为新加的，所以需要复制一份到输出路径
- Status.CHANGED：该文件被修改，所以需要重新复制一份到输出路径

修改完后，我们测试下效果，在执行一次完整编译后，创建一个Test.java 类，再执行一次编译，能看到以下打印结果：  
```
> Task :app:transformClassesWithWTransformForExpDebug
isIncremental ==> true
jar input ==> /Users/wubohua/work/project/Android/Application_decoupling2/annotation/build/libs/annotation.jar
dstFile ==> /Users/wubohua/work/project/Android/Application_decoupling2/app/build/intermediates/transforms/WTransform/exp/debug/2.jar
jar input ==> /Users/wubohua/work/project/Android/Application_decoupling2/businessexp/build/intermediates/runtime_library_classes/debug/classes.jar
dstFile ==> /Users/wubohua/work/project/Android/Application_decoupling2/app/build/intermediates/transforms/WTransform/exp/debug/0.jar
jar input ==> /Users/wubohua/work/project/Android/Application_decoupling2/businesslayer/build/intermediates/runtime_library_classes/debug/classes.jar
dstFile ==> /Users/wubohua/work/project/Android/Application_decoupling2/app/build/intermediates/transforms/WTransform/exp/debug/1.jar
directory input ==> /Users/wubohua/work/project/Android/Application_decoupling2/app/build/intermediates/javac/expDebug/compileExpDebugJavaWithJavac/classes
dstDir ==> /Users/wubohua/work/project/Android/Application_decoupling2/app/build/intermediates/transforms/WTransform/exp/debug/3
change file: /Users/wubohua/work/project/Android/Application_decoupling2/app/build/intermediates/javac/expDebug/compileExpDebugJavaWithJavac/classes/com/wbh/decoupling/Test.class, status: ADDED
```
修改Test.java的代码，比如添加个方法后，再执行编译，能看到如下打印：  
```
> Task :app:transformClassesWithWTransformForExpDebug
isIncremental ==> true
jar input ==> /Users/wubohua/work/project/Android/Application_decoupling2/annotation/build/libs/annotation.jar
dstFile ==> /Users/wubohua/work/project/Android/Application_decoupling2/app/build/intermediates/transforms/WTransform/exp/debug/2.jar
jar input ==> /Users/wubohua/work/project/Android/Application_decoupling2/businessexp/build/intermediates/runtime_library_classes/debug/classes.jar
dstFile ==> /Users/wubohua/work/project/Android/Application_decoupling2/app/build/intermediates/transforms/WTransform/exp/debug/0.jar
jar input ==> /Users/wubohua/work/project/Android/Application_decoupling2/businesslayer/build/intermediates/runtime_library_classes/debug/classes.jar
dstFile ==> /Users/wubohua/work/project/Android/Application_decoupling2/app/build/intermediates/transforms/WTransform/exp/debug/1.jar
directory input ==> /Users/wubohua/work/project/Android/Application_decoupling2/app/build/intermediates/javac/expDebug/compileExpDebugJavaWithJavac/classes
dstDir ==> /Users/wubohua/work/project/Android/Application_decoupling2/app/build/intermediates/transforms/WTransform/exp/debug/3
change file: /Users/wubohua/work/project/Android/Application_decoupling2/app/build/intermediates/javac/expDebug/compileExpDebugJavaWithJavac/classes/com/wbh/decoupling/Test.class, status: CHANGED
```
将Test.java 文件删除后，再执行编译，打印结果如下：  
```
> Task :app:transformClassesWithWTransformForExpDebug
isIncremental ==> true
jar input ==> /Users/wubohua/work/project/Android/Application_decoupling2/annotation/build/libs/annotation.jar
dstFile ==> /Users/wubohua/work/project/Android/Application_decoupling2/app/build/intermediates/transforms/WTransform/exp/debug/2.jar
jar input ==> /Users/wubohua/work/project/Android/Application_decoupling2/businessexp/build/intermediates/runtime_library_classes/debug/classes.jar
dstFile ==> /Users/wubohua/work/project/Android/Application_decoupling2/app/build/intermediates/transforms/WTransform/exp/debug/0.jar
jar input ==> /Users/wubohua/work/project/Android/Application_decoupling2/businesslayer/build/intermediates/runtime_library_classes/debug/classes.jar
dstFile ==> /Users/wubohua/work/project/Android/Application_decoupling2/app/build/intermediates/transforms/WTransform/exp/debug/1.jar
directory input ==> /Users/wubohua/work/project/Android/Application_decoupling2/app/build/intermediates/javac/expDebug/compileExpDebugJavaWithJavac/classes
dstDir ==> /Users/wubohua/work/project/Android/Application_decoupling2/app/build/intermediates/transforms/WTransform/exp/debug/3
change file: /Users/wubohua/work/project/Android/Application_decoupling2/app/build/intermediates/javac/expDebug/compileExpDebugJavaWithJavac/classes/com/wbh/decoupling/Test.class, status: REMOVED
```

至此，讲完了 Transform 的基本用法，而且 WTransform 现在实现的逻辑只是拷贝输入的代码到输出。  

#### 获取注解生成的类
在遍历 jar 文件的时候，同时遍历获取其中以 `com.wbh.decoupling.generate` 为开头的所有类并收集起来：

```groovy
private static final String TARGET = 'com/wbh/decoupling/generate/'
private static List<String> sTargetList = new ArrayList<>()

private static void transformJar(JarInput jarInput, TransformOutputProvider outputProvider, boolean isIncremental) {
    File dstFile = outputProvider.getContentLocation(
            jarInput.getName(),
            jarInput.getContentTypes(),
            jarInput.getScopes(),
            Format.JAR)

    JarFile jarFile = new JarFile(jarInput.file)
    println(jarFile.name)
    jarFile.entries().each {
        if (it.name.contains(TARGET)) {
            sTargetList.add(it.name)
        }
    }
    // ... ...
}
```
在收集到注解生成的类文件后，然后通过生成固定的class文件，在这个类文件里去调用这几个注解生成的类文件：  
```java
// com.wbh.decoupling.generate

public class AsmCrossCompileUtils {

    public static void init() {
        CardManager_becf3fc7606c9b461025f1def7ff27ac.init();
        CardManager_dc2db21188334cfca97494d99700395.init();
    }
}
```
接下来的主要任务就是生成这个类文件。

#### ASM的引入
ASM 是一个 Java 字节码操控框架，我们通过这个框架来生成字节码文件，使用这个框架需要对字节码规范有一定的了解，这里不做详解。  

为了方便，这里需要用到一个studio插件`ASM Bytecode Outline`，这个插件可以用来查看一个类文件编译后的字节码文件，以及生成这个字节码文件的 ASM 代码，安装过程略。  


我们先自己编写`AsmCrossCompileUtils.java`这个类，然后右键选择`Show Bytecode outline`
![](https://note.youdao.com/yws/api/personal/file/WEB61d756bf97fd6e70615b7c8a675d1c59?method=download&shareKey=f3188d416bc3210825b67e7e58de1a3b)

然后会对这个文件自动编译生成字节码文件：  
![](https://note.youdao.com/yws/api/personal/file/WEBbb7e3e9f82225e78b4435dfe669a42e4?method=download&shareKey=0f5f1893f34933f4300bc3895deb04be)  

我们将生成的 ASM 代码复制到 WPlugin 里，同时还需要引入依赖 `implementation 'org.ow2.asm:asm:6.0'`    

```java
import java.util.*;

import org.objectweb.asm.*;

public class AsmCrossCompileUtilsDump implements Opcodes {

    public static byte[] dump() throws Exception {

        ClassWriter cw = new ClassWriter(0);
        FieldVisitor fv;
        MethodVisitor mv;
        AnnotationVisitor av0;

        cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, "com/wbh/decoupling/generate/AsmCrossCompileUtils", null, "java/lang/Object", null);

        cw.visitSource("AsmCrossCompileUtils.java", null);

        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitLineNumber(3, l0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLocalVariable("this", "Lcom/wbh/decoupling/generate/AsmCrossCompileUtils;", null, l0, l1, 0);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "init", "()V", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitLineNumber(6, l0);
            mv.visitMethodInsn(INVOKESTATIC, "com/wbh/decoupling/generate/CardManager_becf3fc7606c9b461025f1def7ff27ac", "init", "()V", false);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLineNumber(7, l1);
            mv.visitMethodInsn(INVOKESTATIC, "com/wbh/decoupling/generate/CardManager_dc2db21188334cfca97494d99700395", "init", "()V", false);
            Label l2 = new Label();
            mv.visitLabel(l2);
            mv.visitLineNumber(8, l2);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }
}
```
分析下这份 ASM 代码，dump() 方法里的第一块代码块是在生成目标类 AsmCrossCompileUtils 的构造器字节码，第二块代码块是在生成 init() 方法，我们整理下这份代码：  
```groovy
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
    
    // ASM 框架生成类 AsmCrossCompileUtils 的字节码
    private static byte[] dump(List<String> list) throws Exception {

        ClassWriter cw = new ClassWriter(0)
        cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, CLASS_FULL_NAME, null, "java/lang/Object", null)
        cw.visitSource(JAVA_FILE_NAME, null)
        visitConstructionMethod(cw)
        visitInitMethod(cw, list)
        cw.visitEnd()

        return cw.toByteArray()
    }

    // 生成构造器代码
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
    
    // 生成 init 静态方法，并在方法中调用 @Param list 指定类的 init() 方法
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
```
这里的逻辑会将 ASM 框架生成的字节码文件保存到 Transform 的输出目录。然后我们需要在 WTransform 代码里调用这里的逻辑：  
```groovy
class WTransform extends Transform {
    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        sTargetList.clear()
        // ... ...
        injectClass(outputProvider)
    }
    
    private static void injectClass(TransformOutputProvider outputProvider) {
        AsmCrossCompileUtilsDump.injectClass(outputProvider, sTargetList)
    }
}
```
至此，Transform的所有逻辑已经写完了，然后我们自己写的 AsmCrossCompileUtils 类也需要删除，最后重新编译发布我们的插件，重新运行代码可以看到 Activity 能正常显示注册的各个 Card。  
