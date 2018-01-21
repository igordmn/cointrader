import numpy as np

from src.cointrader.util.indicators import compute_max_drawdown, sharpe, positive_count, negative_count


def backtest(setname, agent, matrix, config, log):
    x, price_inc, prices, w = matrix.get_validation_set() if setname == "validation" else matrix.get_test_set()
    total_steps = x.shape[0]

    def normalize_portfolio(portfolio):
        return portfolio / np.sum(portfolio)

    def trade_single(step, portfolio):
        history = x[step][np.newaxis, :, :, :]

        train_prices = np.concatenate((np.ones(1), prices[step]))

        portfolio_percents = normalize_portfolio(portfolio)
        result = np.squeeze(agent.best_portfolio(history, portfolio_percents[np.newaxis, 1:]))
        new_portfolio_percents = normalize_portfolio(result)
        log("suggestion", ", ".join("%.2f" % f for f in new_portfolio_percents))
        current_index = portfolio_percents.tolist().index(max(portfolio_percents))
        buy_index = new_portfolio_percents.tolist().index(max(new_portfolio_percents))

        if buy_index != current_index:
            current_price = train_prices[current_index]
            buy_price = train_prices[buy_index]

            if current_index != 0:
                old_amount = portfolio[current_index]
                portfolio[current_index] = 0.0
                portfolio[0] = old_amount * current_price * (1 - config.fee)

            if buy_index != 0 and portfolio[0] > 0:
                old_amount = portfolio[0]
                portfolio[0] = 0.0
                portfolio[buy_index] = old_amount / buy_price * (1 - config.fee)

    def compute_capital(step, portfolio):
        current_prices = np.concatenate((np.ones(1), prices[step]))
        sum = 0.0
        for i in range(0, len(portfolio)):
            sum += portfolio[i] * current_prices[i]
        return sum

    def trade_all():
        portfolio = np.zeros((config.coin_number + 1))
        portfolio[0] = 1.0
        capitals = []

        for step in range(0, total_steps - 1):
            capital = compute_capital(step, portfolio)
            # log("=" * 30)
            # log("the step is {}".format(step))
            trade_single(step, portfolio)
            # log("portfolio is " + ", ".join("%.2f" % f for f in portfolio))
            # log('capital is %3f BTC' % capital)
            capitals.append(capital)

        periods_per_day = int(24 * 60 * 60 / config.period)
        total_periods = total_steps - 1
        total_profit = compute_capital(total_periods - 1, portfolio)
        one_day_profit = total_profit ** (periods_per_day / total_periods)
        ten_days_profit = total_profit ** (10 * periods_per_day / total_periods)
        # log("10 days profit", ten_days_profit)
        log("1 day profit", one_day_profit)

        profits = np.array([nxt / current for current, nxt in zip(capitals, capitals[1:])])
        log("maximum drawdown", compute_max_drawdown(profits))
        log("sharpe", sharpe(profits))
        log("positive_count", positive_count(profits))
        log("negative_count", negative_count(profits))

        return one_day_profit, capitals

    return trade_all()

