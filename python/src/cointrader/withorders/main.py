from typing import NamedTuple

import tensorflow as tf
import pandas as pd
import numpy as np

from src.cointrader.withorders.nnagent_orders import NNAgentOrders

batch_size = 200
history_size = 320
steps = 20000
log_steps = 1000
period = 60
periods_per_day = int(24 * 60 * 60 / period)
data = pd.read_csv("D:/Development/Projects/cointrader/gdaxBTCUSD.csv")
agent = NNAgentOrders(3, history_size)

before_percent = 1.0
test_minutes = 4 * 24 * 60

end = int(data.shape[0] * before_percent)
train_start = 0
train_end = end - test_minutes
test_start = train_end
test_end = end


class DataBatch:
    def __init__(self, history, close_prices, next_high_prices, next_low_prices):
        self.history = history
        self.close_prices = close_prices
        self.next_high_prices = next_high_prices
        self.next_low_prices = next_low_prices


def submatrix(start, end, columns):
    return data.loc[start:end-1, columns].values


def batch(start, end):
    history = np.array([submatrix(i - history_size + 1, i + 1, ["close", "high", "low"]) for i in range(start, end)])
    close_prices = submatrix(start, end, "close")
    next_high_prices = submatrix(start + 1, end + 1, "high")
    next_low_prices = submatrix(start + 1, end + 1, "low")

    return DataBatch(history, close_prices, next_high_prices, next_low_prices)


def random_batch(start, end):
    batch_end = np.random.randint(start + batch_size + history_size, end - 1)
    batch_start = batch_end - batch_size
    return batch(batch_start, batch_end)


total_capital_increase_per_day = 1

for i in range(steps):
    train_batch = random_batch(train_start, train_end)
    log_capital_increase = agent.train(train_batch.history, train_batch.close_prices, train_batch.next_high_prices,
                                       train_batch.next_low_prices)
    capital_increase = np.exp(log_capital_increase)
    capital_increase_per_period = capital_increase ** (1 / batch_size)
    capital_increase_per_day = capital_increase_per_period ** periods_per_day
    total_capital_increase_per_day *= capital_increase_per_day
    if i % log_steps == 0 or i == steps - 1:
        test_batch = random_batch(test_start, test_end)
        test_log_capital_increase = agent.test(test_batch.history, test_batch.close_prices, test_batch.next_high_prices,
                                               test_batch.next_low_prices)
        test_capital_increase_per_period = test_log_capital_increase ** (1 / batch_size)
        test_capital_increase_per_day = test_capital_increase_per_period ** periods_per_day
        print('step %d' % i)
        print('1 day profit (train)', total_capital_increase_per_day ** (1 / log_steps))
        print('1 day profit', test_capital_increase_per_day)

        total_capital_increase_per_day = 1
