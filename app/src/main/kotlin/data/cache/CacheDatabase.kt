package data.cache

import java.sql.Connection
import java.sql.DriverManager

fun connectCacheDatabase(): Connection {
    Class.forName("org.sqlite.JDBC")
    return DriverManager.getConnection("jdbc:sqlite:data/cache.db")
}

fun updateCacheDatabase(connection: Connection) {
    connection.createStatement().use {
        it.executeUpdate("""
            CREATE TABLE IF NOT EXISTS History (date INTEGER,
                            coin varchar(20), high FLOAT, low FLOAT,
                            open FLOAT, close FLOAT
                           PRIMARY KEY (date, coin))
        """.trimIndent())
    }
}