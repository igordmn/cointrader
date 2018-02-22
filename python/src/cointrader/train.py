import os
import shutil

import numpy as np
from os.path import dirname, abspath

from src.cointrader.util.backtest import backtest
from src.cointrader.util.config import TrainConfig
from src.cointrader.util.plot import plot_log
from src.cointrader.util.train import train_net
from src.cointrader.constants import *
from src.cointrader.util.nnagent import NNAgent, train_config_to_nn
from src.cointrader.util.datamatrices import DataMatrices


def clear_train_dir(path):
    dir = dirname(abspath(path))
    if os.path.exists(dir):
        shutil.rmtree(dir)
    os.makedirs(dir)


clear_train_dir(NET_FILE)
clear_train_dir(NET_FILE_MAX)

np.random.seed(284112293)

config = TrainConfig()

agent = NNAgent(train_config_to_nn(config))
matrix = DataMatrices(DATABASE_DIR, config)


def save_max(step, agent):
    path = NET_FILE_MAX + step
    agent.save(path)


try:
    train_net(agent, matrix, config, print, save_max)
    agent.save(NET_FILE)
    # one_day_profit, capitals = backtest(agent, matrix, config, print)
    # plot_log(capitals, config)
finally:
    agent.recycle()

