package data

import java.io.File

val f = File("C:/Users/Igor/Desktop/pol_1x_adam0.00084_20160101_0.0040.txt")

fun main(args: Array<String>) {
    println(f.readLines().filter {
        it.startsWith("1 day profit (test) ")
    }.map {
                it.removePrefix("1 day profit (test) ").toDouble()
            }.map { it.toString().replace(".", ",") }.joinToString("\n"))
}