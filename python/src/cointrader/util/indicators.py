import numpy as np


def compute_max_drawdown(profits):
    max_drawdown = 0
    max_capital = 0
    capital = 1
    for profit in profits:
        capital *= profit
        if capital >= max_capital:
            max_capital = capital
        else:
            drawdown = 1.0 - capital / max_capital
            if drawdown > max_drawdown:
                max_drawdown = drawdown
    return max_drawdown


def sharpe(profits):
    log_profits = np.log(profits)
    return np.mean(log_profits) / np.std(log_profits)


def standard_deviation(profits):
    return np.std(np.log(profits))


def moving_accumulate(profits, n=48):
    acc = np.cumprod(profits)
    acc[n:] = acc[n:] / acc[:-n]
    return acc


def positive_count(profits):
    return np.sum(profits > 1)


def negative_count(profits):
    return np.sum(profits < 1)
