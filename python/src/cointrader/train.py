import os
import shutil

import numpy as np

from src.cointrader.util.backtest import backtest
from src.cointrader.util.config import TrainConfig
from src.cointrader.util.plot import plot_log
from src.cointrader.util.train import train_net
from src.cointrader.constants import *
from src.cointrader.util.nnagent import NNAgent
from src.cointrader.util.datamatrices import DataMatrices

if os.path.exists("data/train_package"):
    shutil.rmtree("data/train_package")
os.makedirs("data/train_package")

np.random.seed(284112293)

config = TrainConfig()

agent = NNAgent(
    config.indicator_number, 1 + config.coin_number, config.window_size
)
matrix = DataMatrices(DATABASE_DIR, config)

try:
    train_net(agent, matrix, config, print)
    agent.save(NET_FILE)
    one_day_profit, capitals = backtest("validation", agent, matrix, config, print)
    plot_log(capitals, config)
finally:
    agent.recycle()

