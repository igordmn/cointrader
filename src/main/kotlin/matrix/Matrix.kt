package matrix

import matrix.gpu.MultiplyMatrixKernel
import matrix.gpu.ReLUMatrixKernel
import java.util.*

private val multiplyKernel = ThreadLocal.withInitial(::MultiplyMatrixKernel)
private val reluKernel = ThreadLocal.withInitial(::ReLUMatrixKernel)

data class Matrix(val rows: Int, val cols: Int, val data: DoubleArray) {
    infix operator fun times(other: Matrix): Matrix {
        require(cols == other.rows)
        return Matrix(
                rows,
                other.cols,
                multiplyKernel.get().apply(data, other.data, rows, cols, other.cols)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Matrix

        if (rows != other.rows) return false
        if (cols != other.cols) return false
        if (!Arrays.equals(data, other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rows
        result = 31 * result + cols
        result = 31 * result + Arrays.hashCode(data)
        return result
    }
}

fun reLU(matrix: Matrix): Matrix {
    return Matrix(
            matrix.rows,
            matrix.cols,
            reluKernel.get().apply(matrix.data, matrix.rows, matrix.cols)
    )
}

fun randomMatrix(rows: Int, cols: Int) = Matrix(
        rows,
        cols,
        DoubleArray(rows * cols) { Math.random() * 2 - 1 }
)

fun addIdentityCol(matrix: Matrix): Matrix {
    var matrixI = 0
    var newCol = 0
    val newData = DoubleArray(matrix.data.size + matrix.rows) {
        val isLastCol = newCol == matrix.cols
        val res = if (isLastCol) 1.0 else matrix.data[matrixI]

        newCol++

        if (newCol == matrix.cols + 1) {
            newCol = 0
        } else {
            matrixI++
        }

        res
    }
    return Matrix(matrix.rows, matrix.cols + 1, newData)
}