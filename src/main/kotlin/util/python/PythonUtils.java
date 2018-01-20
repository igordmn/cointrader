package util.python;

import org.jpy.PyLib;
import org.jpy.PyModule;
import org.jpy.PyObject;

import java.lang.reflect.Array;
import java.nio.file.Paths;

public class PythonUtils {
    public static void startPython() {
        String rootPath = Paths.get("").toAbsolutePath().toString();
        String envPath = "E:/Distr/Portable/Dev/Anaconda3/envs/coin_predict"; // todo убрать хардкод
        System.setProperty("jpy.jpyLib", rootPath + "/lib/native/jpy.cp36-win_amd64.pyd");
        System.setProperty("jpy.pythonLib", envPath + "/python36.dll");
        System.setProperty("jpy.pythonPrefix", envPath);
        System.setProperty("jpy.pythonExecutable", envPath + "/python.exe");
        PyLib.startPython();
        PyModule.extendSysPath(rootPath + "/python", true);
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