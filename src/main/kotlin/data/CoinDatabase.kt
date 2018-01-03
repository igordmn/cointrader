package data

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.ThreadLocalTransactionManager
import java.math.BigDecimal
import java.sql.Connection

val COIN_DATABASE_FILE = "D:/1/coins.db"

fun connectCoinDatabase() {
    Database.connect(
            "jdbc:sqlite:$COIN_DATABASE_FILE",
            driver = "org.sqlite.JDBC",
            manager = { ThreadLocalTransactionManager(it, Connection.TRANSACTION_SERIALIZABLE) }
    )
}

object Histories : Table("History") {
    val exchange: Column<String> = varchar("exchange", length = 20).primaryKey()
    val coin: Column<String> = varchar("coin", length = 20).primaryKey()
    val date: Column<Long> = long("date").primaryKey()
    val open: Column<BigDecimal> = decimal("open", 40, 20)
    val close: Column<BigDecimal> = decimal("close", 40, 20)
    val high: Column<BigDecimal> = decimal("high", 40, 20)
    val low: Column<BigDecimal> = decimal("low", 40, 20)
    val volume: Column<BigDecimal> = decimal("volume", 40, 20)
}

data class History(
        val exchange: String,
        val coin: String,
        val date: Long,
        val open: BigDecimal,
        val close: BigDecimal,
        val high: BigDecimal,
        val low: BigDecimal,
        val volume: BigDecimal
) {
    init {
        require(date in 151493761..15149375999)
        require(high >= low)
        require(high >= open)
        require(high >= close)
        require(low <= open)
        require(low <= close)
        require(open > BigDecimal.ZERO)
        require(close > BigDecimal.ZERO)
        require(high > BigDecimal.ZERO)
        require(low > BigDecimal.ZERO)
        require(volume >= BigDecimal.ZERO)
    }
}

fun deleteHistories(exchange: String, coin: String) {
    Histories.deleteWhere {
        (Histories.exchange eq exchange) and (Histories.coin eq coin)
    }
}

fun insertHistory(history: History) {
    Histories.insert {
        it[Histories.exchange] = history.exchange
        it[Histories.coin] = history.coin
        it[Histories.date] = history.date
        it[Histories.open] = history.open
        it[Histories.close] = history.close
        it[Histories.high] = history.high
        it[Histories.low] = history.low
        it[Histories.volume] = history.volume
    }
}