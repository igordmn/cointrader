def train_net(agent, matrix, config, log):
    def log_results(i, total_train_profit):
        total_profit = 1.0
        batch_count = 0
        for batch in matrix.test_batches():
            batch_profit = agent.test(batch)[0]
            total_profit *= batch_profit
            batch_count += 1

        periods_per_day = int(24 * 60 * 60 / config.period)

        log('step %d' % i)
        log('1 day profit (train)', (total_train_profit ** (1 / config.log_steps)) ** periods_per_day)
        log('1 day profit (test)', (total_train_profit ** (1 / batch_count)) ** periods_per_day)
        log('\n')

    def train():
        total_profit = 1.0

        i = 0
        for batch in matrix.train_batches():
            batch_profit = agent.train(batch)[0]
            total_profit *= batch_profit
            if i % config.log_steps == 0 or i == config.steps - 1:
                log_results(i, total_profit)
                total_profit = 1.0

            i += 1
            if i == config.steps:
                break

    train()
