package read

import java.io.File

fun readCoinbaseByMin(): List<Double> =
        File("D:/1/bitstampUSD.csv/coinbaseUSD_1-min_data_2014-12-01_to_2017-10-20.csv.csv")
                .reader()
                .useLines { sequence ->
                    sequence.drop(1).map {
                        it.split(',')
                    }.map {
                        it[4].toDouble()
                    }.toList()
                }
