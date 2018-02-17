import sys

from src.cointrader.util.backtest import backtest
from src.cointrader.util.config import TrainConfig
from src.cointrader.util.plot import plot_log
from src.cointrader.constants import *
from src.cointrader.util.nnagent import NNAgent, train_config_to_nn
from src.cointrader.util.datamatrices import DataMatrices

config = TrainConfig()

matrix = DataMatrices(DATABASE_DIR, config)
agent = NNAgent(train_config_to_nn(config), NET_FILE)

try:
    one_day_profit, capitals = backtest(agent, matrix, config, print)
    plot_log(capitals, config)
finally:
    agent.recycle()
