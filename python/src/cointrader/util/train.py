import numpy as np


def train_net(agent, matrix, config, log):
    def log_results(i, train_period_profit):
        total_profit = 1.0
        batch_count = 0
        for batch in matrix.test_batches():
            batch_profit = agent.test(batch)[0]
            total_profit *= batch_profit
            batch_count += 1

        periods_per_day = int(24 * 60 * 60 / config.period)

        test_period_profit = total_profit ** (1 / batch_count)
        train_day_profit = train_period_profit ** periods_per_day
        test_day_profit = test_period_profit ** periods_per_day
        log(f'{i}   {train_day_profit}   {test_day_profit}')

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
        return np.sum(new_portfolio * price_incs) - np.sum(np.abs(new_portfolio - old_portoflio)) * config.fee

    periods_per_day = int(24 * 60 * 60 / config.period)

    total_train_profit = 1.0
    total_test_profit = 1.0
    portfolio = np.zeros((1 + len(config.coins)))
    portfolio[0] = 1.0

    result_periods = config.sequential_result_days * periods_per_day
    result_start_step = matrix.train_sequential_end() - result_periods
    result_profit = 1.0
    result_all_profit = 1.0

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

        if i % config.log_steps == 0:
            train_day_profit = total_train_profit ** (periods_per_day / config.log_steps / config.sequential_steps)
            test_day_profit = total_test_profit ** (periods_per_day / config.log_steps)
            log(f'{i}   {train_day_profit}   {test_day_profit}')
            total_train_profit = 1.0
            total_test_profit = 1.0

    all_periods = matrix.train_sequential_end() - matrix.train_sequential_start()

    return [result_all_profit ** (periods_per_day / all_periods), result_profit ** (periods_per_day / result_periods)]
