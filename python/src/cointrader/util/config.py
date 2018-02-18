import time
from datetime import datetime
from typing import NamedTuple


# select coin, sum(volume)/30 total_volume from History where date >= 1512086400 and date < 1514678400 and exchange="binance" group by coin order by total_volume desc
# select coin, sum(volume)/30 total_volume from History where date >= 1495238400 and date < 1497916800 and exchange="bittrex" group by coin order by total_volume desc
# select * from (select coin, min(date) as mn from History where exchange="binance" group by coin) where mn >= 1514764800


def parse_time(time_string):
    return int(time.mktime(datetime.strptime(time_string, "%Y/%m/%d %H:%M:%S").timetuple()))

binance_coins = [
    # "USDT", "BCH", "ETH", "NEO", "IOTA", "ETC", "WTC", "QSP", "EOS",
    # "QTUM", "XRP", "LTC", "HSR", "OMG", "STRAT", "MTL", "XMR", "SUB",
    # "LSK", "ZEC"

    "USDT", "ETH", "TRX", "XRP", "LTC", "ETC", "ICX", "VEN", "NEO", "ADA", "XLM", "HSR", "EOS",
    "BCH", "LSK", "POE", "PPT", "APPC", "MTL", "IOTA", "WTC", "ENG", "ZRX", "XVG", "OMG", "BRD",
    "ADX", "KNC", "DGD", "QTUM", "ZEC", "GXS", "XMR", "CND", "LEND", "STRAT", "VIBE", "BTG", "ELF",
    "FUN", "BTS", "AION", "DASH", "GAS"
]

poloniex_coins = [
    "XRP", "USDT", "ETH", "LTC", "XLM", "XMR", "NXT",
    "XEM", "DASH", "DGB", "ETC", "DOGE", "EMC2", "SC", "LSK",
    "BTS", "ZEC", "STRAT", "FCT", "REP", "ARDR", "VTC",
    "BCN", "BURST", "MAID", "STEEM", "SYS", "POT", "NAV",
    "DCR", "LBC", "FLDC", "GAME"
]

class TrainConfig(NamedTuple):
    exchange: str = "binance"
    exchange_db_period = 60
    coins: list = binance_coins
    start_time: int = parse_time("2017/8/1 00:00:00")
    end_time: int = parse_time("2018/2/18 11:40:00")
    test_days: float = 10
    sequential_result_days = 30
    steps: int = 80000
    log_steps: int = 2000
    indicators: list = sorted(["close", "high", "low"])
    fee: float = 0.0014

    period: int = 300
    geometric_bias: float = 5e-06
    window_size: int = 160
    batch_size: int = 109
    sequential_steps: int = 8
    sequential_bias: float = 5e-03
    learning_rate: float = 0.00028
    weight_decay: float = 5e-6
    use_batch_normalization: bool = True
    dropout: float = 0.45
    conv_size: int = 12
    conv_kernel: int = 5
    dense_size: int = 32

    max_network_min_steps: int = 10000

    indicator_number: int = len(indicators)
    coin_number: int = len(coins)
