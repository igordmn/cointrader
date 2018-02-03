package train

import exchange.candle.CoinToCandles

class TrainData(
        private val data: FloatArray,
        private val coins: List<String>,
        val size: Int
) {
    suspend fun get(from: Int, to: Int): CoinToCandles {
        require(from in 0 until size)
        require(to in 0 until size)
        require(to >= from)
        TODO()
    }
}