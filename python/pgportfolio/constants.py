DATABASE_DIR = "D:/Development/Projects/cointrader/db/coins.db"
NET_FILE = "./train_package/netfile"


# select coin, sum(volume)/30 total_volume from History where date >= 1512086400 and date < 1514678400 and exchange="binance" group by coin order by total_volume desc
# select coin, sum(volume)/30 total_volume from History where date >= 1495238400 and date < 1497916800 and exchange="bittrex" group by coin order by total_volume desc
# select * from (select coin, min(date) as mn from History where exchange="binance" group by coin) where mn < 1514764800


ALL_COINS = {
    "binance": [
        "USDT", "TRX", "ETH", "NEO", "XRP", "BCD", "ADA", "ICX", "CND", "XVG", "XLM", "WTC",
        "NEBL", "HSR", "POE", "GAS", "ETC", "QTUM", "TRIG",
        "TNB", "LEND", "QSP", "XMR", "BTS", "OMG", "STRAT", "LSK",
        "FUN", "MANA", "FUEL", "AION", "ENJ", "MCO", "CDT"
    ],
    # "binance": [
    #     "USDT", "ETH", "MCO", "NEO", "OMG", "QTUM", "STRAT", "XVG", "DASH", "ETC", "SNT", "TRX", "LTC", "XRP", "IOTA", "BCH"
    # ],
    # "binance": [
    #         "USDT", "TRX", "ETH", "NEO", "XRP", "BNB", "BCD", "VEN", "ADA", "EOS",
    #         "ICX", "CND", "XVG", "BCH", "XLM", "WTC", "LTC", "NEBL", "ELF", "INS",
    #         "IOTA", "HSR", "POE", "GAS", "ETC", "APPC", "ARN", "QTUM", "VIBE", "BTG",
    #         "TRIG", "TNB", "LEND", "QSP", "XMR", "BTS", "ZRX", "OMG", "SUB", "STRAT",
    #         "GTO", "BRD", "WABI", "LRC", "LSK", "FUN", "MANA", "ENG", "KNC", "BQX",
    #         "POWR", "FUEL", "ZEC", "AION", "REQ", "DASH", "ENJ", "BAT", "OST", "MCO",
    #         "CDT", "CTR", "LINK", "SNT", "AST", "SALT", "CMT"
    # ],
    "poloniex": [
          'ETH', 'XMR', 'FCT', 'ETC', 'USDT', 'STR', 'XRP', 'LTC'
    ],
    # "poloniex": [
    #     "XRP", "USDT", "ETH", "BCH", "LTC", "XLM", "XMR", "NXT",
    #     "XEM", "DASH", "DGB", "ETC", "DOGE", "EMC2", "SC", "LSK",
    #     "BTS", "ZEC", "STRAT", "FCT", "REP", "ARDR", "VTC",
    #     "BCN", "BURST", "MAID", "STEEM", "SYS", "POT", "NAV",
    #     "DCR", "LBC", "FLDC", "GAME"
    # ],
    "bittrex": [
        "ETH", "XRP", "DGB", "ETC", "STRAT", "LTC", "XVG",
        "RDD", "XEM", "WAVES", "DOGE", "ZEC", "GNT", "DASH",
        "LSK", "USDT", "NXT", "STEEM", "XMR", "PIVX", "XLM", "ANT",
        "UBQ", "ARK", "SYS", "LBC", "WINGS", "GAME", "FCT",
        "KMD", "ARDR", "XDN", "HMQ", "VOX", "DCR", "BITB"
    ],
    "bitfinex": [
        "USD", "BCH", "ETH",  "IOTA", "XRP", "LTC", "EOS",
        "XMR", "DASH", "ETC", "ZEC", "NEO", "OMG", "BTG", "QTUM",
        "SAN", "ETP", "RRT", "DAT"
    ]
}
