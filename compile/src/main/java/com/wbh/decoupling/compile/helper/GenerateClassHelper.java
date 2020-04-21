package com.wbh.decoupling.compile.helper;


import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;


/**
 * Created by wubohua on 2019/6/23.
 *
 */

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
