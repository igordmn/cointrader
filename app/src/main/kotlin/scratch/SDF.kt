package scratch

import jep.DirectNDArray
import jep.Jep
import python.jep
import java.nio.ByteBuffer
import java.util.*
import kotlin.system.measureNanoTime

fun main(args: Array<String>) {
    jep().use { jep ->
        jep.eval("""
            def gg(x):
                return x
            """.trimIndent())
        val r = Random()

        println(measureNanoTime {
            test(r, jep)
        } / 1e6)
        Thread.sleep(1000)
        println(measureNanoTime {
            test(r, jep)
        } / 1e6)
        Thread.sleep(1000)
        println(measureNanoTime {
            test(r, jep)
        } / 1e6)
        Thread.sleep(1000)
        println(measureNanoTime {
            test(r, jep)
        } / 1e6)
    }
}
val size = 109 * 160 * 4 * 50
val darr1 = ByteBuffer.allocateDirect(size * 4).asFloatBuffer()

private fun test(r: Random, jep: Jep) {
    darr1.position(0)
    for (i in 1..size) {
            darr1.put(6F)
    }
    val narr1 = DirectNDArray(darr1)
    jep.invoke("gg", narr1)
}