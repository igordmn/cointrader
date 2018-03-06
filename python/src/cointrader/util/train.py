import numpy as np

from src.cointrader.util.backtest import backtest


def train_net(agent, matrix, config, log, save_max):
    def empty_print(*_):
        pass

    max_result = 0.0
    result_mul = 1.0
    result_count = 0

    def log_results(i, train_period_profit):
        nonlocal max_result
        nonlocal result_mul
        nonlocal result_count

        periods_per_day = int(24 * 60 * 60 / config.period)
        test_day_profit_allin, _ = backtest(agent, matrix, config, empty_print, use_allin=True)
        test_day_profit, _ = backtest(agent, matrix, config, empty_print, use_allin=False)
        train_day_profit = train_period_profit ** periods_per_day
        log(f'{i}   {train_day_profit}   {test_day_profit_allin}   {test_day_profit}')

        if test_day_profit_allin > max_result and i >= config.max_network_min_steps:
            max_result = test_day_profit_allin
            save_max(i, agent)

        result_mul *= test_day_profit_allin
        result_count += 1

    def train():
        total_profit = 1.0

        i = 0
        for batch in matrix.train_batches():
            batch_profit = agent.train(batch)[0]
            total_profit *= batch_profit
            if i % config.log_steps == 0 or i == config.steps - 1:
                log_results(i, total_profit ** (1 / config.log_steps))
                total_profit = 1.0

            i += 1
            if i == config.steps:
                break

    train()

    avg_result = result_mul ** (1 / result_count)
    return avg_result, max_result, 0
