package old.exchange.binance

// should be singleton because of maxRequestsPerSecond
val binanceAPI = old.exchange.binance.api.binanceAPI()