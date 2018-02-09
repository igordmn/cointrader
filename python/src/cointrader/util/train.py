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
