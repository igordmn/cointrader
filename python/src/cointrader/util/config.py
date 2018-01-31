import time
from datetime import datetime
from typing import NamedTuple

# select coin, sum(volume)/30 total_volume from History where date >= 1512086400 and date < 1514678400 and exchange="binance" group by coin order by total_volume desc
# select coin, sum(volume)/30 total_volume from History where date >= 1495238400 and date < 1497916800 and exchange="bittrex" group by coin order by total_volume desc
# select * from (select coin, min(date) as mn from History where exchange="binance" group by coin) where mn >= 1514764800



def parse_time(time_string):
    return int(time.mktime(datetime.strptime(time_string, "%Y/%m/%d %H:%M:%S").timetuple()))


class TrainConfig(NamedTuple):
    exchange: str = "binance"
    coins: list = [
        "USDT", "TRX", "ETH", "XRP", "VEN", "NEO",
        "EOS", "BCD", "ICX", "WTC", "ELF", "CND",
        "ADA", "XLM", "BCH", "XVG", "LTC", "HSR",
        "NEBL", "IOTA", "ETC", "QTUM", "POE", "BTG",
        "TNB", "ZRX", "LRC", "TNT", "LEND", "GTO",
        "OMG", "BRD", "SUB", "BTS", "WABI", "XMR",
        "OST", "AION", "ENJ", "STRAT", "ENG", "AMB",
        "LSK", "AST", "CDT", "MDA", "LINK", "DASH",
        "KNC", "MTL"
    ]
    coin_number: int = len(coins)
    validation_portion: float = 0.01
    test_portion: float = 0.2
    fee: float = 0.0022
    window_size: int = 160
    batch_size: int = 109
    steps: int = 80000
    log_steps: int = 1000
    period: int = 300
    start_time: int = parse_time("2017/8/1 00:00:00")
    end_time: int = parse_time("2018/1/31 18:00:00")
    indicators: list = sorted(["close", "high", "low"])
    indicator_number: int = len(indicators)
    geometric_bias: float = 5e-07
    use_geometric_sample: bool = True
