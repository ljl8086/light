package com.na.light.hessian;

import com.caucho.hessian.io.AbstractStringValueDeserializer;

import java.math.BigDecimal;

/**
 * Created by sunny on 2017/8/21 0021.
 */
public class BigDecimalDeserializer extends AbstractStringValueDeserializer {

    @Override
    public Class getType() {
        return BigDecimal.class;
    }

    @Override
    protected Object create(String value) {
        if (null != value) {
            return new BigDecimal(value);
        } else {
            return null;
        }
    }
}