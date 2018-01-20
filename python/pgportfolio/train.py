import os
import shutil

import numpy as np

from pgportfolio.agent.backtest import backtest
from pgportfolio.agent.config import TrainConfig
from pgportfolio.agent.plot import plot_log
from pgportfolio.agent.train import train_net
from pgportfolio.constants import *
from pgportfolio.agent.nnagent import NNAgent
from pgportfolio.agent.datamatrices import DataMatrices

print(os.getcwd())

if os.path.exists("data/train_package"):
    shutil.rmtree("data/train_package")
os.makedirs("data/train_package")

np.random.seed(0)

config = TrainConfig()

matrix = DataMatrices(DATABASE_DIR, config)
agent = NNAgent(
    config.fee, config.indicator_number, config.coin_number, config.window_size
)

try:
    train_net(agent, matrix, config, print)
    agent.save(NET_FILE)
    one_day_profit, capitals = backtest("validation", agent, matrix, config, print)
    plot_log(capitals, config)
finally:
    agent.recycle()

