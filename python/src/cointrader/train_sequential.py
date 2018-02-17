import os
import shutil

import numpy as np
from os.path import dirname, abspath

from src.cointrader.util.backtest import backtest
from src.cointrader.util.config import TrainConfig
from src.cointrader.util.plot import plot_log
from src.cointrader.util.train import train_net, train_net_sequential
from src.cointrader.constants import *
from src.cointrader.util.nnagent import NNAgent, train_config_to_nn
from src.cointrader.util.datamatrices import DataMatrices

dir = dirname(abspath(NET_FILE))

if os.path.exists(dir):
    shutil.rmtree(dir)
os.makedirs(dir)

np.random.seed(284112293)

config = TrainConfig()

agent = NNAgent(train_config_to_nn(config))
matrix = DataMatrices(DATABASE_DIR, config)

try:
    result_all, result_last, capitals = train_net_sequential(agent, matrix, config, print)
    print(f"{result_all} {result_last}")
    plot_log(capitals, config)
    agent.save(NET_FILE)
finally:
    agent.recycle()
