package util.math

class DoubleMatrix4D(
        val n1: Int,
        val n2: Int,
        val n3: Int,
        val n4: Int,
        value: (i1: Int, i2: Int, i3: Int, i4: Int) -> Double
) {
    val data: DoubleArray = DoubleArray(n1 * n2 * n3 * n4)

    init {
        var k = 0
        for (i1 in 0 until n1)
            for (i2 in 0 until n2)
                for (i3 in 0 until n3)
                    for (i4 in 0 until n4)
                        data[k++] = value(i1, i2, i3, i4)
    }
}

class DoubleMatrix2D(
        val n1: Int,
        val n2: Int,
        value: (i1: Int, i2: Int) -> Double
) {
    val data: DoubleArray = DoubleArray(n1 * n2)

    init {
        var k = 0
        for (i1 in 0 until n1)
            for (i2 in 0 until n2)
                data[k++] = value(i1, i2)
    }

    fun forEach(accept: (i1: Int, i2: Int, value: Double) -> Unit) {
        var k = 0
        for (i1 in 0 until n1)
            for (i2 in 0 until n2)
                accept(i1, i2, data[k++])
    }
}