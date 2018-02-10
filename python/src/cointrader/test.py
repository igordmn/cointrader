import sys

from src.cointrader.util.backtest import backtest
from src.cointrader.util.config import TrainConfig
from src.cointrader.util.plot import plot_log
from src.cointrader.constants import *
from src.cointrader.util.nnagent import NNAgent
from src.cointrader.util.datamatrices import DataMatrices

config = TrainConfig()

matrix = DataMatrices(DATABASE_DIR, config)
agent = NNAgent(config, NET_FILE)

try:
    one_day_profit, capitals = backtest(agent, matrix, config, print)
    plot_log(capitals, config)
finally:
    agent.recycle()
