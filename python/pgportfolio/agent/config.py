import time
from datetime import datetime
from typing import NamedTuple

from pgportfolio.constants import ALL_COINS


def parse_time(time_string):
    return int(time.mktime(datetime.strptime(time_string, "%Y/%m/%d %H:%M:%S").timetuple()))


class TrainConfig(NamedTuple):
    exchange: str = "binance"
    validation_portion: float = 0.0001
    test_portion: float = 0.0
    fee: float = 0.001
    window_size: int = 160
    batch_size: int = 109
    coin_number: int = 34
    steps: int = 35000
    log_steps: int = 1000
    period: int = 300
    start_time: int = parse_time("2017/8/1 00:00:00")
    end_time: int = parse_time("2018/1/19 12:00:00")
    indicators: list = sorted(["close"])
    indicator_number: int = len(indicators)
    geometric_bias: float = 5e-07
    use_geometric_sample: bool = True
    coins: list = ALL_COINS[exchange][:coin_number]
    aproximate_buy_sell_price: bool = True