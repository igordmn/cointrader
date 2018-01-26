package exchange

import exchange.history.MarketHistory

class Market(val broker: MarketBroker, val history: MarketHistory, val price: MarketPrice)