import numpy as np

from src.cointrader.util.indicators import compute_max_drawdown, sharpe_ratio, positive_count, negative_count, standard_deviation, \
    sortino_ratio, downside_deviation


def backtest(agent, matrix, config, log):
    test_set = matrix.get_test_set()
    x = test_set.x
    all_prices = test_set.prices
    total_steps = x.shape[0]

    def normalize_portfolio(portfolio):
        return portfolio / np.sum(portfolio)

    def rebalance(step, portfolio, new_portfolio_percents):
        capital = np.sum(portfolio)
        desired_portfolio = capital * new_portfolio_percents

        diffs = desired_portfolio - portfolio
        buys = diffs.clip(min=0)
        sells = (-diffs).clip(min=0)
        total_fee = np.sum(buys * config.fee) + np.sum(sells * config.fee)
        capital_after_fee = capital - total_fee

        return capital_after_fee * new_portfolio_percents

    def rebalance_limit(portfolio, portfolio_percents, new_portfolio_percents, prices, next_high_prices, next_low_prices):
        current_index = portfolio_percents.tolist().index(max(portfolio_percents))
        buy_index = new_portfolio_percents.tolist().index(max(new_portfolio_percents))

        if buy_index != current_index:
            current_price = prices[current_index]
            buy_price = prices[buy_index]
            can_sell = next_low_prices[current_index] <= current_price <= next_high_prices[current_index]
            can_buy = next_low_prices[buy_index] <= buy_price <= next_high_prices[buy_index]

            if current_index != 0 and can_sell:
                old_amount = portfolio[current_index]
                portfolio[current_index] = 0.0
                portfolio[0] = old_amount * current_price * (1 - config.fee)

            if buy_index != 0 and portfolio[0] > 0 and can_buy:
                old_amount = portfolio[0]
                portfolio[0] = 0.0
                portfolio[buy_index] = old_amount / buy_price * (1 - config.fee)

        return portfolio

    def trade_single(step, portfolio):
        history = x[step][np.newaxis, :, :, :]

        prices = all_prices[step]
        next_high_prices = test_set.x[step + 1, 1, :, -1]
        next_low_prices = test_set.x[step + 1, 2, :, -1]

        portfolio_btc = portfolio * prices
        portfolio_percents = normalize_portfolio(portfolio_btc)
        result = np.squeeze(agent.best_portfolio(history, portfolio_percents[np.newaxis, :]))
        new_portfolio_percents = normalize_portfolio(result)
        log("portfolio", ", ".join("%.2f" % f for f in new_portfolio_percents))

        # portfolio_btc = rebalance(step, portfolio_btc, new_portfolio_percents)
        # return portfolio_btc / prices

        return rebalance_limit(portfolio, portfolio_percents, new_portfolio_percents, prices, next_high_prices, next_low_prices)

    def compute_capital(step, portfolio):
        current_prices = all_prices[step]
        sum = 0.0
        for i in range(0, len(portfolio)):
            sum += portfolio[i] * current_prices[i]
        return sum

    def trade_all():
        portfolio = np.zeros((1 + len(config.coins)))
        portfolio[0] = 1.0
        capitals = []

        for step in range(0, total_steps - 1):
            capital = compute_capital(step, portfolio)
            portfolio = trade_single(step, portfolio)
            capitals.append(capital)

        periods_per_day = int(24 * 60 * 60 / config.period)
        total_periods = total_steps - 1
        total_profit = compute_capital(total_periods - 1, portfolio)
        one_day_profit = total_profit ** (periods_per_day / total_periods)
        log("1 day profit", one_day_profit)

        profits = np.array([nxt / current for current, nxt in zip(capitals, capitals[1:])])

        log("maximum drawdown", compute_max_drawdown(profits))
        log("sharpe_ratio", sharpe_ratio(profits))
        log("sortino_ratio", sortino_ratio(profits))
        log("standard_deviation", standard_deviation(profits))
        log("downside_deviation", downside_deviation(profits))
        log("positive_count", positive_count(profits))
        log("negative_count", negative_count(profits))

        return one_day_profit, capitals

    return trade_all()
