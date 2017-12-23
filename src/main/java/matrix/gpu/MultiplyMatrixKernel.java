package matrix.gpu;

import com.aparapi.Kernel;

/**
 * This class contains method run which will be converted from java bytecode to OpenCL bytecode
 */
public class MultiplyMatrixKernel extends Kernel {
    double[] res;
    double[] A;
    double[] B;
    int a;
    int b;
    int c;

    @Override
    public void run() {
        int k = getGlobalId();
        int row = k / c;
        int col = k % c;
        for (int i = 0; i < b; i++) {
            res[k] += A[row * b + i] * B[i * c + col];
        }
    }

    public double[] apply(final double[] A, final double[] B, final int a, final int b, final int c) {
        this.A = A;
        this.B = B;
        this.a = a;
        this.b = b;
        this.c = c;
        this.res = new double[a * c];
        execute(res.length);
        return res;
    }
}
