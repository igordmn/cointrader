import gc
import traceback

import numpy as np

from src.cointrader.util.backtest import backtest
from src.cointrader.util.config import TrainConfig
from src.cointrader.util.train import train_net
from src.cointrader.constants import *
from src.cointrader.util.nnagent import NNAgent
from src.cointrader.util.datamatrices import DataMatrices
import logging
import time


def geo_mean(iterable):
    a = np.array(iterable)
    return a.prod() ** (1.0 / len(a))


def empty_print(*_):
    pass


def test_with_config(config):
    matrix = DataMatrices(DATABASE_DIR, config)
    agent = NNAgent(
        config.indicator_number, 1 + config.coin_number, config.window_size
    )
    try:
        train_net(agent, matrix, config, empty_print)
        one_day_profit, capitals = backtest("validation", agent, matrix, config, empty_print)
        return one_day_profit
    except Exception:
        print(traceback.format_exc())
        return -1
    finally:
        agent.recycle()


log = logging.getLogger('myapp')
formatter = logging.Formatter('%(message)s')
hdlr = logging.FileHandler('batch.log')
hdlr.setFormatter(formatter)
log.addHandler(hdlr)
log.setLevel(logging.INFO)


def log_info(*args):
    log.info(" ".join([str(arg) for arg in args]))


def print_default_config():
    c = TrainConfig()
    log_info("")
    log_info("")
    log_info("exc", c.exchange, "fee", c.fee, "coins", c.coins)
    log_info("start", c.start_time, "end", c.end_time)


def print_config(c, result):
    log_info(
        "v", c.validation_portion, "t", c.test_portion,
        "s", c.steps, "p", c.period,
        "b", c.batch_size, "w", c.window_size, "i", c.indicator_number,
        "geom", c.use_geometric_sample, "geom_bias", c.geometric_bias,
        "RESULT", result
    )


configs = [

]

test_count = 5

print_default_config()
log_info("")

total_num = len(configs)
num = 0
for config in configs:
    num += 1

    start = time.time()

    result = geo_mean([test_with_config(config) for x in range(test_count)])
    print_config(config, result)
    gc.collect()

    end = time.time()
    print(num, total_num, end - start)
