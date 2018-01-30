import sys

from src.cointrader.util.backtest import backtest
from src.cointrader.util.config import TrainConfig
from src.cointrader.util.plot import plot_log
from src.cointrader.constants import *
from src.cointrader.util.nnagent import NNAgent
from src.cointrader.util.datamatrices import DataMatrices

config = TrainConfig()

if len(sys.argv) > 1:
    matrix = DataMatrices(DATABASE_DIR, config)
    agent = NNAgent(
        config.fee, config.indicator_number, 1 + config.coin_number, config.window_size,
        NET_FILE
    )
    try:
        one_day_profit, capitals = backtest(sys.argv[1], agent, matrix, config, print)
        plot_log(capitals, config)
    finally:
        agent.recycle()
