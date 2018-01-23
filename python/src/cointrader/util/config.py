import time
from datetime import datetime
from typing import NamedTuple

from src.cointrader.constants import ALL_COINS


# select coin, sum(volume)/30 total_volume from History where date >= 1512086400 and date < 1514678400 and exchange="binance" group by coin order by total_volume desc
# select coin, sum(volume)/30 total_volume from History where date >= 1495238400 and date < 1497916800 and exchange="bittrex" group by coin order by total_volume desc
# select * from (select coin, min(date) as mn from History where exchange="binance" group by coin) where mn >= 1514764800



def parse_time(time_string):
    return int(time.mktime(datetime.strptime(time_string, "%Y/%m/%d %H:%M:%S").timetuple()))


class TrainConfig(NamedTuple):
    exchange: str = "binance"
    coins: list = [
        "USDT", "ETH", "CND", "VEN", "TRX", "EOS", "XRP", "WTC", "TNT", "BNB",
        "ICX", "NEO", "XLM", "ELF", "LEND", "ADA", "LTC", "XVG", "IOTA",
        "HSR", "TNB", "BCH", "BCD", "CTR", "POE", "ETC", "QTUM", "MANA",
        "OMG", "BRD", "AION", "AMB", "SUB", "ZRX", "BTS", "STRAT", "WABI",
        "LINK", "XMR", "QSP", "LSK", "GTO", "ENG", "MCO", "POWR", "CDT",
        "KNC", "REQ", "OST", "ENJ", "DASH"
    ]
    coin_number: int = len(coins)
    validation_portion: float = 0.0001
    test_portion: float = 0.0
    fee: float = 0.0023
    window_size: int = 160
    batch_size: int = 109
    steps: int = 80000
    log_steps: int = 1000
    period: int = 300
    start_time: int = parse_time("2017/8/1 00:00:00")
    end_time: int = parse_time("2018/1/23 17:55:00")
    indicators: list = sorted(["close", "high", "low"])
    indicator_number: int = len(indicators)
    geometric_bias: float = 5e-07
    use_geometric_sample: bool = True
    aproximate_buy_sell_price: bool = True
