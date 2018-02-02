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
        # "USDT", "ETH", "TRX", "NEO", "VEN", "XRP", "ICX", "EOS",
        # "ELF", "WTC", "CND", "ADA", "XLM", "XVG", "HSR", "LTC",
        # "BCH", "ETC", "IOTA", "POE", "BTG", "QTUM", "TNT", "LSK",
        # "GAS", "VIB", "ZRX", "OMG", "LEND", "BRD", "GTO", "BTS",
        # "SUB", "XMR", "AION", "LRC", "STRAT", "MDA", "ENJ", "QSP",
        # "WABI", "KNC", "CMT", "REQ", "AST", "MTL", "DASH", "ZEC",
        # "WINGS"

        "USDT", "ETH", "TRX", "NEO", "VEN", "XRP", "EOS", "WTC", "ADA",
        "XVG", "HSR", "LTC", "BCH", "ETC", "IOTA", "POE", "BTG",
        "TNT", "QTUM", "LSK", "GAS", "VIB", "ZRX", "OMG", "BTS",
        "SUB", "XMR", "LRC", "STRAT", "MDA", "ENJ", "KNC", "REQ",
        "AST", "ENG", "ZEC", "DGD", "ADX", "BQX", "SALT"
    ]
    coin_number: int = len(coins)
    validation_portion: float = 0.01
    test_portion: float = 0.05
    fee: float = 0.0019
    window_size: int = 160
    batch_size: int = 109
    steps: int = 30000
    log_steps: int = 1000
    period: int = 900
    start_time: int = parse_time("2017/8/1 00:00:00")
    end_time: int = parse_time("2018/2/2 13:40:00")
    indicators: list = sorted(["close", "high", "low"])
    indicator_number: int = len(indicators)
    geometric_bias: float = 5e-07
    use_geometric_sample: bool = True
