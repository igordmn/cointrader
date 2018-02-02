import numpy as np

from src.cointrader.util.indicators import compute_max_drawdown, sharpe_ratio, positive_count, negative_count, standard_deviation, \
    sortino_ratio, downside_deviation


def backtest(setname, agent, matrix, config, log):
    x, price_inc, all_prices, buy_fees, sell_fees, w = matrix.get_validation_set() if setname == "validation" else matrix.get_test_set()
    total_steps = x.shape[0]

    def normalize_portfolio(portfolio):
        return portfolio / np.sum(portfolio)

    def rebalance(step, portfolio, new_portfolio_percents):
        capital = np.sum(portfolio)
        desired_portfolio = capital * new_portfolio_percents

        diffs = desired_portfolio - portfolio
        buys = diffs.clip(min=0)
        sells = (-diffs).clip(min=0)
        total_fee = np.sum(buys * 0.0019) + np.sum(sells * 0.0019)
        capital_after_fee = capital - total_fee

        return capital_after_fee * new_portfolio_percents

    def trade_single(step, portfolio):
        history = x[step][np.newaxis, :, :, :]

        prices = all_prices[step]

        portfolio_btc = portfolio * prices
        portfolio_percents = normalize_portfolio(portfolio_btc)
        result = np.squeeze(agent.best_portfolio(history, portfolio_percents[np.newaxis, :]))
        new_portfolio_percents = normalize_portfolio(result)
        log("portfolio", ", ".join("%.2f" % f for f in new_portfolio_percents))

        portfolio_btc = rebalance(step, portfolio_btc, new_portfolio_percents)
        return portfolio_btc / prices

    def compute_capital(step, portfolio):
        current_prices = all_prices[step]
        sum = 0.0
        for i in range(0, len(portfolio)):
            sum += portfolio[i] * current_prices[i]
        return sum

    def trade_all():
        portfolio = np.zeros((1 + config.coin_number))
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
