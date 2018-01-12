package util.python;

import org.jpy.PyLib;
import org.jpy.PyModule;
import org.jpy.PyObject;

import java.lang.reflect.Array;

public class PythonUtils {
    public static void startPython() {
        System.setProperty("jpy.jpyLib", "D:/Development/Projects/cointrader/lib/native/jpy.cp36-win_amd64.pyd");
        System.setProperty("jpy.pythonLib", "E:/Distr/Portable/Dev/Anaconda3/envs/coin_predict/python36.dll");
        System.setProperty("jpy.pythonPrefix", "E:/Distr/Portable/Dev/Anaconda3/envs/coin_predict");
        System.setProperty("jpy.pythonExecutable", "E:/Distr/Portable/Dev/Anaconda3/envs/coin_predict/python.exe");
        PyLib.startPython();
        PyModule.extendSysPath("D:\\Development\\Projects\\coin_predict", true);
    }

    public static void stopPython() {
        PyLib.stopPython();
    }

    public static double[] getDoubleArrayValue(PyObject obj) {
        Object[] raw = obj.getObjectArrayValue(Object.class);
        double[] data = new double[raw.length];
        for (int i = 0; i< raw.length; i++) {
            data[i] = (Double) raw[i];
        }
        return data;
    }
}