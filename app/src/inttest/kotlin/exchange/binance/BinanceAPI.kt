package exchange.binance

// should be singleton because of maxRequestsPerSecond
val binanceAPI = exchange.binance.api.binanceAPI()