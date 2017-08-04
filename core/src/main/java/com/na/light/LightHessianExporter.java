package com.na.light;

import org.springframework.remoting.caucho.HessianExporter;
import org.springframework.remoting.caucho.HessianServiceExporter;

/**
 * Created by Administrator on 2017/8/4 0004.
 */
public class LightHessianExporter extends HessianServiceExporter {
    @Override
    protected ClassLoader getBeanClassLoader() {
        System.out.println("=================--222------------------------===============");
        return this.getClass().getClassLoader();
    }
}
