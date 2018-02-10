import time
from datetime import datetime
from typing import NamedTuple


# select coin, sum(volume)/30 total_volume from History where date >= 1512086400 and date < 1514678400 and exchange="binance" group by coin order by total_volume desc
# select coin, sum(volume)/30 total_volume from History where date >= 1495238400 and date < 1497916800 and exchange="bittrex" group by coin order by total_volume desc
# select * from (select coin, min(date) as mn from History where exchange="binance" group by coin) where mn >= 1514764800


def parse_time(time_string):
    return int(time.mktime(datetime.strptime(time_string, "%Y/%m/%d %H:%M:%S").timetuple()))


binance_coins = [
    # "USDT", "ETH", "TRX", "ICX", "VEN", "XRP", "NEO", "EOS", "ELF", "CND", "WTC", "XLM", "ADA", "HSR", "LTC", "VIBE", "XVG", "BCH", "LSK",
    # "IOTA", "POE", "BCD", "ETC", "BRD", "QTUM", "ZRX", "OMG", "BTS", "LEND", "GAS", "SUB", "APPC", "AION", "STRAT", "XMR", "FUN", "TRIG",
    # "ENJ", "ENG", "BTG", "DASH"

    "USDT", "ETH", "TRX", "NEO", "VEN", "XRP", "EOS", "WTC", "ADA",
    "XVG", "HSR", "LTC", "BCH", "ETC", "IOTA", "POE", "BTG",
    "TNT", "QTUM", "LSK", "GAS", "VIB", "ZRX", "OMG", "BTS",
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
    start_time: int = parse_time("2017/11/1 00:00:00")
    end_time: int = parse_time("2018/2/10 16:00:00")
    test_days: float = 1
    sequential_result_days = 20
    steps: int = 20000
    log_steps: int = 250
    indicators: list = sorted(["close", "high", "low"])
    fee: float = 0.0019

    period: int = 240
    geometric_bias: float = 5e-07
    window_size: int = 160
    batch_size: int = 80
    sequential_steps: int = 1
    sequential_bias: float = 5e-03
    learning_rate: float = 0.00028 * 6
    weight_decay: float = 5e-6
    conv_size: int = 8
    conv_kernel: int = 3
    dense_size: int = 32
