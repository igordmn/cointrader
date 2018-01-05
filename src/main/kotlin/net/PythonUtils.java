package net;

import org.jpy.PyObject;

import java.lang.reflect.Array;

public class PythonUtils {
    public static double[] getDoubleArrayValue(PyObject j) {
        Object[] raw = j.getObjectArrayValue(Object.class);

        double[] data = new double[raw.length];
        for (int i = 0; i< raw.length; i++) {
            data[i] = (Double) raw[i];
        }

        return data;
    }
}
