package com.wbh.decoupling.businesslayer;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import dalvik.system.DexFile;

/**
 * Created by wubohua on 2019/6/30.
 */

public class CrossCompileUtils {

    private static final String GENERATE_CODE_PACKAGE_NAME = "com.wbh.decoupling.generate";
    private static final String METHOD_NAME = "init";

    public static void init(Context context) {
        initByAsm();
    }

    private static void initByAsm() {
        try {
            Class cls = Class.forName("com.wbh.decoupling.generate.AsmCrossCompileUtils");
            Method method = cls.getMethod(METHOD_NAME);
            method.invoke(null);
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

    private void initByFoundInDex(Context context) {
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
