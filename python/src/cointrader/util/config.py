import time
from datetime import datetime
from typing import NamedTuple


# select coin, sum(volume)/30 total_volume from History where date >= 1512086400 and date < 1514678400 and exchange="binance" group by coin order by total_volume desc
# select coin, sum(volume)/30 total_volume from History where date >= 1495238400 and date < 1497916800 and exchange="bittrex" group by coin order by total_volume desc
# select * from (select coin, min(date) as mn from History where exchange="binance" group by coin) where mn >= 1514764800


def parse_time(time_string):
    return int(time.mktime(datetime.strptime(time_string, "%Y/%m/%d %H:%M:%S").timetuple()))


binance_coins = [
    "USDT", "BCH", "ETH", "NEO", "IOTA", "ETC", "WTC", "QSP", "EOS",
    "QTUM", "XRP", "LTC", "HSR", "OMG", "STRAT", "MTL", "XMR", "SUB",
    "LSK", "ZEC"
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
    start_time: int = parse_time("2017/9/1 00:00:00")
    end_time: int = parse_time("2018/2/13 00:00:00")
    test_days: float = 1
    sequential_result_days = 23
    steps: int = 20000
    log_steps: int = 250
    indicators: list = sorted(["close", "high", "low"])
    fee: float = 0.0019

    period: int = 840
    geometric_bias: float = 5e-07
    window_size: int = 320
    batch_size: int = 10
    sequential_steps: int = 1
    sequential_bias: float = 5e-03
    learning_rate: float = 0.00028 * 6
    weight_decay: float = 5e-6
    use_batch_normalization: bool = True
    dropout: float = 0.5
    conv_size: int = 8
    conv_kernel: int = 3
    dense_size: int = 32
