package matrix.gpu;

import matrix.gpu.MultiplyMatrixKernel;

import java.util.Random;

public class TEST {
    public static void main(String[] arg) {
        test(500, 500, 500);
        test(1000, 1000, 1000);
        test(1500, 1500, 1500);
        test(2000, 2000, 2000);
        test(2500, 2500, 2500);
    }

    static MultiplyMatrixKernel kernel = new MultiplyMatrixKernel();

    public static void test(int a, int b, int c) {
        Random rnd = new Random();
        double[] A = new double[a * b];
        double[] B = new double[b * c];
        for (int i = 0; i < A.length; i++) {
            A[i] = rnd.nextDouble();
        }
        for (int i = 0; i < B.length; i++) {
            B[i] = rnd.nextDouble();
        }
        long ms = Integer.MAX_VALUE;
        for (int i = 0; i < 10; i++) {
            long start = System.currentTimeMillis();
            double[] res = kernel.apply(A, B, a, b, c);
            long end = System.currentTimeMillis();
            ms = Math.min(ms, end - start);
        }
        System.out.println("GPU [" + a + "," + b + "]X[" + b + "," + c + "] " + ms + " ms");

//        int [] res1 = apply(A, B, a, b, c);
//        int [] res2 = multCpu(A, B, a, b, c);
//        testMatrix(res1, res2, a, c);

//        ms = Integer.MAX_VALUE;
//        for (int i = 0; i < 2; i++) {
//            long start = System.currentTimeMillis();
//            multCpu(A, B, a, b, c);
//            long end = System.currentTimeMillis();
//            ms = Math.min( ms, end - start );
//        }
//        System.out.println( "CPU [" + a + "," + b +"]X[" + b + "," + c + "] " + ms + " ms" );
    }

    public static double[] multCpu(final double[] A, final double[] B, final int a, final int b, final int c) {
        final double[] rezultat = new double[a * c];
        for (int k = 0; k < rezultat.length; k++) {
            int linie = k / c;
            int coloana = k % c;
            for (int i = 0; i < b; i++) {
                rezultat[k] += A[linie * b + i] * B[i * c + coloana];
            }
        }
        return rezultat;
    }

    public static void testMatrix(double[] m1, double[] m2, int a, int b) {
        for (int i = 0; i < a; i++) {
            for (int j = 0; j < b; j++) {
                if (m1[i * b + j] != m2[i * b + j])
                    System.out.println("AAAAAAAA");
            }
        }
    }
}