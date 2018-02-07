def train_net(agent, matrix, config, log):
    test_x, test_prices_incs, test_prices, test_buy_fees, test_sell_fees, test_w = matrix.get_test_set()

    total_train_profit = 1

    for i in range(config.steps):
        batch = matrix.next_batch()
        if batch is None:
            break
        else:
            x = batch.x
            price_inc = batch.price_inc
            buy_fees = batch.buy_fees
            sell_fees = batch.sell_fees
            previous_w = batch.previous_w
            predict_w, train_geometric_mean_profit = agent.train(x, price_inc, buy_fees, sell_fees, previous_w)
            total_train_profit *= train_geometric_mean_profit
            batch.setw(predict_w)
            if i % config.log_steps == 0 or i == config.steps - 1:
                capital, geometric_mean_profit, log_mean_profit, sharp_ratio, sortino_ratio, standard_profit_deviation, downside_profit_deviation = agent.test(test_x, test_prices_incs, test_buy_fees, test_sell_fees, test_w)
                periods_per_day = int(24 * 60 * 60 / config.period)
                log('step %d' % i)
                # print('capital', capital)
                log('1 day profit (train)', (total_train_profit ** (1 / config.log_steps)) ** periods_per_day)
                log('1 day profit', geometric_mean_profit ** periods_per_day)
                log('standard_deviation', standard_profit_deviation)
                log('downside_deviation', downside_profit_deviation)
                log('sharp_ratio', sharp_ratio)
                log('sortino_ratio', sortino_ratio)
                # print('10 days profit', geometric_mean_profit ** (10 * periods_per_day))
                # print('log_mean_profit', log_mean_profit)
                # print('loss_value', loss)
                log('\n')
                total_train_profit = 1
