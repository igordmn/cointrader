package old.exchange.binance

// should be singleton because of maxRequestsPerSecond
val binanceAPI = com.dmi.cointrader.binance.api.binanceAPI()