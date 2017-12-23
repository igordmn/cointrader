package matrix

import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldEqual
import io.kotlintest.specs.StringSpec

class MatrixSpec : StringSpec({
    "addIdentityCol" {
        val m = Matrix(
                2, 3,
                doubleArrayOf(
                        4.0, 5.0, 6.0,
                        7.0, 8.0, 9.0
                )
        )

        addIdentityCol(m) shouldBe Matrix(
                2, 4,
                doubleArrayOf(
                        4.0, 5.0, 6.0, 1.0,
                        7.0, 8.0, 9.0, 1.0
                )
        )
    }
})