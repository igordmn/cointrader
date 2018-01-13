package trader

interface Trade {
    suspend fun perform()
}