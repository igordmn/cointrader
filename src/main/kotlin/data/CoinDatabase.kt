package data

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.Statement
import org.jetbrains.exposed.sql.statements.StatementType
import org.jetbrains.exposed.sql.transactions.ThreadLocalTransactionManager
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

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
    val openTime: Column<Long> = long("date").primaryKey()
    val closeTime: Column<Long> = long("closeTime").primaryKey()
    val open: Column<BigDecimal> = decimal("open", 40, 20)
    val close: Column<BigDecimal> = decimal("close", 40, 20)
    val high: Column<BigDecimal> = decimal("high", 40, 20)
    val low: Column<BigDecimal> = decimal("low", 40, 20)
    val volume: Column<BigDecimal> = decimal("volume", 40, 20)
}

data class History(
        val exchange: String,
        val coin: String,
        val openTime: Long,
        val closeTime: Long,
        val open: BigDecimal,
        val close: BigDecimal,
        val high: BigDecimal,
        val low: BigDecimal,
        val volume: BigDecimal
) {
    init {
        require(openTime in 151493761..15149375999)
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
        it[Histories.openTime] = history.openTime
        it[Histories.closeTime] = history.closeTime
        it[Histories.open] = history.open
        it[Histories.close] = history.close
        it[Histories.high] = history.high
        it[Histories.low] = history.low
        it[Histories.volume] = history.volume
    }
}

fun loadHistory(exchange: String, coin: String, limit: Int, end: Long, period: Long): List<History> {
    val start = end - period * limit
    require(start % period == 0L)
    require(end % period == 0L)

    val result = transaction {
        Histories.select {
            (Histories.exchange eq exchange) and(Histories.coin eq coin) and
                    (Histories.openTime greaterEq start) and
                    (Histories.openTime less end) and
                    ((Histories.openTime.div(period).times(period)) eq Histories.openTime)
        }.map { History(
                it[Histories.exchange],
                it[Histories.coin],
                it[Histories.openTime],
                it[Histories.closeTime],
                it[Histories.open],
                it[Histories.close],
                it[Histories.high],
                it[Histories.low],
                it[Histories.volume]
        ) }.toList()
    }
    require(result.size == limit)
    require(result.first().openTime == start)
    require(result.last().openTime == end - period)
    return result
}

fun <T:Any> execSQL(sql: String, transform : (ResultSet) -> T) : T? {
    return TransactionManager.current().exec(object : Statement<T>(StatementType.SELECT, emptyList()) {
        override fun PreparedStatement.executeInternal(transaction: Transaction): T? {
            return executeQuery()?.let { transform(it) }
        }

        override fun prepareSQL(transaction: Transaction): String = sql
        override fun arguments(): Iterable<Iterable<Pair<ColumnType, Any?>>> = emptyList()
    })
}