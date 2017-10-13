package com.na.light.hessian;


import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.AbstractSerializer;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Created by Sunny on 2017/8/21 0021.
 */
public class BigDecimalSerializer extends AbstractSerializer {
    @Override
    public void writeObject(Object obj, AbstractHessianOutput out) throws IOException {

        if (obj == null)
            out.writeNull();
        else {
            Class cl = obj.getClass();

            if (out.addRef(obj))
                return;

            int ref = out.writeObjectBegin(cl.getName());

            BigDecimal bi = (BigDecimal) obj;

            if (ref < -1) {
                out.writeString("value");
                out.writeString(bi.toString());
                out.writeMapEnd();
            } else {
                if (ref == -1) {
                    out.writeInt(1);
                    out.writeString("value");
                    out.writeObjectBegin(cl.getName());
                }

                out.writeString(bi.toString());
            }
        }
    }
}
