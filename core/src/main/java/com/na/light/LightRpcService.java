package com.na.light;

import java.lang.annotation.*;

/**
 * Created by sunny on 2017/8/2 0002.
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LightRpcService {
    String value();
}
