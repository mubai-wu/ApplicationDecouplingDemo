package com.wbh.decoupling.compile;

import com.google.auto.service.AutoService;
import com.wbh.decoupling.annotation.Register;
import com.wbh.decoupling.compile.helper.GenerateClassHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

@AutoService(Processor.class)
public class RegisterProcessor extends AbstractProcessor {

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
