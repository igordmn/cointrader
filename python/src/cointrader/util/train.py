import numpy as np

from src.cointrader.util.backtest import backtest


def train_net(agent, matrix, config, log, save_max):
    def empty_print(*_):
        pass

    max_result = 1.0

    def log_results(i, train_period_profit):
        nonlocal max_result

        periods_per_day = int(24 * 60 * 60 / config.period)
        test_day_profit, _ = backtest(agent, matrix, config, empty_print)
        train_day_profit = train_period_profit ** periods_per_day
        log(f'{i}   {train_day_profit}   {test_day_profit}')

        if test_day_profit > max_result and i >= config.max_network_min_steps:
            max_result = test_day_profit
            save_max(agent)

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


def train_net_sequential(agent, matrix, config, log):
    def normalize_portfolio(portfolio):
        return portfolio / np.sum(portfolio)

    def profit(old_portoflio, new_portfolio, price_incs):
        cost = 1 - np.sum(np.abs(new_portfolio - old_portoflio)) * config.fee
        return np.sum(new_portfolio * price_incs) * cost

    periods_per_day = int(24 * 60 * 60 / config.period)

    total_train_profit = 1.0
    total_test_profit = 1.0
    portfolio = np.zeros((1 + len(config.coins)))
    portfolio[0] = 1.0

    result_periods = config.sequential_result_days * periods_per_day
    result_start_step = matrix.train_sequential_end() - result_periods
    result_profit = 1.0
    result_all_profit = 1.0
    result_capital = 1
    result_capitals = []

    for i in range(matrix.train_sequential_start(), matrix.train_sequential_end()):
        for batch in matrix.train_batches_sequential(i, config.sequential_steps):
            train_batch_profit = agent.train(batch)[0]
            total_train_profit *= train_batch_profit

        test_data = matrix.sample_at(i)
        result = np.squeeze(agent.best_portfolio(test_data.x, portfolio[np.newaxis, :]))
        new_portfolio = normalize_portfolio(result)
        test_profit = profit(portfolio, new_portfolio, test_data.price_incs[0])
        total_test_profit *= test_profit
        portfolio = new_portfolio

        if i >= result_start_step:
            result_profit *= test_profit
        result_all_profit *= test_profit
        result_capital *= test_profit
        result_capitals.append(result_capital)

        if i % config.log_steps == 0:
            train_day_profit = total_train_profit ** (periods_per_day / config.log_steps / config.sequential_steps)
            test_day_profit = total_test_profit ** (periods_per_day / config.log_steps)
            log(f'{i}   {train_day_profit}   {test_day_profit}')
            total_train_profit = 1.0
            total_test_profit = 1.0

    all_periods = matrix.train_sequential_end() - matrix.train_sequential_start()

    return result_all_profit ** (periods_per_day / all_periods), result_profit ** (periods_per_day / result_periods), result_capitals
