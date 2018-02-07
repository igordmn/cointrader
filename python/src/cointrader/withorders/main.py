from typing import NamedTuple

import tensorflow as tf
import pandas as pd
import numpy as np

from src.cointrader.withorders.nnagent_orders import NNAgentOrders

batch_size = 200
history_size = 320
df = pd.read_csv("D:/Development/Projects/cointrader/gdaxBTCUSD.csv")
agent = NNAgentOrders(3, history_size)

before_percent = 1.0
test_minutes = 4 * 24 * 60



class DataBatch:
    def __init__(self, history, close_prices, next_high_prices, next_low_prices):
        self.history = history
        self.close_prices = close_prices
        self.next_high_prices = next_high_prices
        self.next_low_prices = next_low_prices

#
# def next_batch():
