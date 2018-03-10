package com.na.light;

/**
 * Created by Administrator on 2017/8/10 0010.
 */
public class LightConfig {

    public static ThreadLocal threadLocal = new ThreadLocal();

    public static void setSelector(Object object) {
        threadLocal.set(object);
    }

    public static ThreadLocal getThreadLocal() {
        return threadLocal;
    }
}
