package com.na.light;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Created by sunny on 2017/8/2 0002.
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LightRpcService {
    /**
     * 接口名称。接口名称不能有/字符.
     * @return
     */
    String value() default "";

    /**
     * 访问路径。
     * @return
     */
    String url() default "";
}
