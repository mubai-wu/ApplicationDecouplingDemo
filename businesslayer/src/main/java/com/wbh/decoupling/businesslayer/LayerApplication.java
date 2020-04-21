package com.wbh.decoupling.businesslayer;

import android.app.Application;


public class LayerApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CrossCompileUtils.init(this);
    }
}
