package matrix.gpu;

import com.aparapi.Kernel;

/**
 * This class contains method run which will be converted from java bytecode to OpenCL bytecode
 */
public class ReLUMatrixKernel extends Kernel {
    double[] res;
    double[] A;

    @Override
    public void run() {
        int k = getGlobalId();
        res[k] = max(A[k], 0.0);
    }

    public double[] apply(final double[] A, final int a, final int b) {
        this.A = A;
        this.res = new double[a * b];
        execute(res.length);
        return res;
    }
}
