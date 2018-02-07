import os
import shutil
from os.path import dirname, abspath

import numpy as np

from src.cointrader.constants import *
from src.cointrader.util.config import TrainConfig
from src.cointrader.util.datamatrices import DataMatrices
from src.cointrader.util.nnagent import NNAgent

dir = dirname(abspath(NET_FILE))

if os.path.exists(dir):
    shutil.rmtree(dir)
os.makedirs(dir)

np.random.seed(284112293)

config = TrainConfig()

agent = NNAgent(
    config.fee,
    config.indicator_number, 1 + config.coin_number, config.window_size
)
matrix = DataMatrices(DATABASE_DIR, config)


def train_net_sequential(agent, matrix, config, log):
    portfolio = np.zeros((1 + config.coin_number))
    portfolio[0] = 1.0

    periods_per_day = int(24 * 60 * 60 / config.period)
    total_train_profit = 1
    total_test_profit = 1
    log_steps = periods_per_day

    def print_result():
        nonlocal total_train_profit
        nonlocal total_test_profit

        log('step %d of %d' % (i, matrix.batch_sequential_count()))
        log('1 day profit (train)', (total_train_profit ** (1 / log_steps)) ** periods_per_day)
        log('1 day profit (test)', (total_test_profit ** (1 / log_steps)) ** periods_per_day)
        log('\n')
        total_train_profit = 1
        total_test_profit = 1

    for i in range(0, 100000000):
        batch = matrix.next_batch_sequential()
        if batch is None:
            print_result()
            break
        else:
            x = batch.x
            price_inc = batch.price_inc
            buy_fees = batch.buy_fees
            sell_fees = batch.sell_fees
            previous_w = batch.previous_w
            train_geometric_mean_profit = 1
            max_profit = np.prod(np.maximum.reduce(price_inc, 1)) * ((1 - 2 * config.fee) ** config.batch_size)
            for j in range(0, 1000):
                predict_w, train_geometric_mean_profit = agent.train(x, price_inc, buy_fees, sell_fees, previous_w)
                train_profit = train_geometric_mean_profit ** config.batch_size
                previous_w = predict_w
            total_train_profit *= train_geometric_mean_profit

            test_x = x[-1, :, :, :]
            test_price_inc = price_inc[-1, :]
            new_portfolio = np.squeeze(agent.best_portfolio(test_x[np.newaxis, :, :, :], portfolio[np.newaxis, :]))
            new_portfolio = new_portfolio / sum(new_portfolio)
            commission = 1 - np.sum(np.abs(new_portfolio - portfolio)) * config.fee
            profit = np.sum(test_price_inc * new_portfolio) * commission
            portfolio = new_portfolio
            total_test_profit *= profit


            if i % log_steps == 0:
                print_result()


try:
    train_net_sequential(agent, matrix, config, print)
    agent.save(NET_FILE)
    # plot_log(capitals, config)
finally:
    agent.recycle()
