import sys

from pgportfolio.agent.backtest import backtest
from pgportfolio.agent.config import TrainConfig
from pgportfolio.agent.plot import plot_log
from pgportfolio.constants import *
from pgportfolio.agent.nnagent import NNAgent
from pgportfolio.agent.datamatrices import DataMatrices

config = TrainConfig()

if len(sys.argv) > 1:
    matrix = DataMatrices(DATABASE_DIR, config)
    agent = NNAgent(
        config.fee, config.indicator_number, config.coin_number, config.window_size,
        NET_FILE
    )
    try:
        one_day_profit, capitals = backtest(sys.argv[1], agent, matrix, config, print)
        plot_log(capitals, config)
    finally:
        agent.recycle()
