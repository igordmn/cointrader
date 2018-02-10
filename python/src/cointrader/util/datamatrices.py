import sqlite3

import numpy as np
import pandas as pd
import gc


def geometricSample(start, end, bias):
    ran = np.random.geometric(bias)
    while ran > end - start:
        ran = np.random.geometric(bias)
    result = end - ran
    return result


def aproximate_price(open, close, high, low, t):
    # t from 0.0 to 1.0

    def value(x1, x2, y1, y2, x):
        return y1 + (y2 - y1) / (x2 - x1) * (x - x1)

    if abs(open - high) <= abs(open - low):
        # chart is open-high-low-close
        return {
            t < 1 / 3: value(0, 1 / 3, open, high, t),
            1 / 3 <= t < 2 / 3: value(1 / 3, 2 / 3, high, low, t),
            2 / 3 <= t: value(2 / 3, 1, low, close, t)
        }[True]
    else:
        # chart is open-low-high-close
        return {
            t < 1 / 3: value(0, 1 / 3, open, low, t),
            1 / 3 <= t < 2 / 3: value(1 / 3, 2 / 3, low, high, t),
            2 / 3 <= t: value(2 / 3, 1, high, close, t)
        }[True]


def random_price(open, close, high, low):
    t = np.random.random_sample()
    return aproximate_price(open, close, high, low, t)


def sell_fee(standard_fee, price, high, low):
    return standard_fee  # 1 - low / price * (1 - standard_fee)


def buy_fee(standard_fee, price, high, low):
    return standard_fee  # 1 - price / high * (1 - standard_fee)


def get_global_panel(database_dir, config):
    def panel_fillna(panel):
        frames = {}
        for item in panel.items:
            frames[item] = panel.loc[item].fillna(axis=1, method="bfill").fillna(axis=1, method="ffill")

        return pd.Panel(frames)

    start = int(config.start_time - (config.start_time % config.period))
    end = int(config.end_time - (config.end_time % config.period))

    time_index = range(start - config.period, end + 1, config.period)
    panel_indicators = config.indicators.copy()
    panel_indicators.append("z_price")
    # panel_indicators.append("zz_buy_fee")
    # panel_indicators.append("zzz_sell_fee")

    panel = pd.Panel(items=panel_indicators, major_axis=config.coins, minor_axis=time_index, dtype=np.float32)

    connection = sqlite3.connect(database_dir)

    try:
        for row_number, coin in enumerate(config.coins):
            for indicator in config.indicators:
                if indicator == "close":
                    sql = ("SELECT closeTime-{period} AS date_norm, close FROM History WHERE"
                           " date>={start} and date<={end}"
                           " and closeTime%{period}=0 and exchange=\"{exchange}\" and coin=\"{coin}\"".format(
                        start=start, end=end, period=config.period, exchange=config.exchange, coin=coin))
                elif indicator == "open":
                    sql = ("SELECT date AS date_norm, open FROM History WHERE"
                           " date_norm>={start} and date_norm<={end}"
                           " and date_norm%{period}=0 and exchange=\"{exchange}\" and coin=\"{coin}\"".format(
                        start=start, end=end, period=config.period, exchange=config.exchange, coin=coin))
                elif indicator == "volume":
                    sql = ("SELECT date_norm, SUM(volume)" +
                           " FROM (SELECT round(date/{period}-0.5)*{period} AS date_norm, volume, coin, exchange FROM History)"
                           " WHERE date_norm>={start} and date_norm<={end} and exchange=\"{exchange}\" and coin=\"{coin}\""
                           " GROUP BY date_norm".format(
                               period=config.period, start=start, exchange=config.exchange, end=end, coin=coin))
                elif indicator == "high":
                    sql = ("SELECT date_norm, MAX(high)" +
                           " FROM (SELECT round(date/{period}-0.5)*{period} AS date_norm, high, coin, exchange FROM History)"
                           " WHERE date_norm>={start} and date_norm<={end} and exchange=\"{exchange}\" and coin=\"{coin}\""
                           " GROUP BY date_norm".format(
                               period=config.period, start=start, exchange=config.exchange, end=end, coin=coin))
                elif indicator == "low":
                    sql = ("SELECT date_norm, MIN(low)" +
                           " FROM (SELECT round(date/{period}-0.5)*{period} AS date_norm, low, coin, exchange FROM History)"
                           " WHERE date_norm>={start} and date_norm<={end} and exchange=\"{exchange}\" and coin=\"{coin}\""
                           " GROUP BY date_norm".format(
                               period=config.period, start=start, exchange=config.exchange, end=end, coin=coin))
                else:
                    raise ValueError("The indicator %s is not supported" % indicator)

                serial_data = pd.read_sql_query(sql, con=connection, index_col="date_norm")
                panel.loc[indicator, coin, serial_data.index] = serial_data.squeeze()
                panel = panel_fillna(panel)

            sql = ("SELECT closeTime-{exchange_db_period}-{period} AS date_norm, open, close, high, low FROM History WHERE"
                   " date>={start} and date<={end}"
                   " and (closeTime-{exchange_db_period})%{period}=0 and exchange=\"{exchange}\" and coin=\"{coin}\"".format(
                start=start, end=end, period=config.period, exchange=config.exchange, exchange_db_period=config.exchange_db_period, coin=coin))

            gc.collect()

            serial_data = pd.read_sql_query(sql, con=connection, index_col="date_norm")
            serial_data['z_price'] = serial_data['open']  # = serial_data.apply(lambda row: row['low'], axis=1)
            # serial_data['z_price'] = serial_data.apply(lambda row: random_price(row['open'], row['close'], row['high'], row['low']), axis=1)
            # serial_data['zz_buy_fee'] = serial_data.apply(
            #     lambda row: buy_fee(config.fee, row['z_price'], row['high'], row['low']), axis=1)
            # serial_data['zzz_sell_fee'] = serial_data.apply(
            #     lambda row: sell_fee(config.fee, row['z_price'], row['high'], row['low']), axis=1)

            panel.loc["z_price", coin, serial_data.index] = serial_data.drop(
                columns=['open', 'close', 'high', 'low']).squeeze()
            # panel.loc["z_price", coin, serial_data.index] = serial_data.drop(
            #     columns=['open', 'close', 'high', 'low', 'zz_buy_fee', 'zzz_sell_fee']).squeeze()
            # panel.loc["zz_buy_fee", coin, serial_data.index] = serial_data.drop(
            #     columns=['open', 'close', 'high', 'low', 'z_price', 'zzz_sell_fee']).squeeze()
            # panel.loc["zzz_sell_fee", coin, serial_data.index] = serial_data.drop(
            #     columns=['open', 'close', 'high', 'low', 'z_price', 'zz_buy_fee']).squeeze()

            panel = panel_fillna(panel)

    finally:
        connection.commit()
        connection.close()

    return panel


class DataBatch:
    def __init__(self, x, price_incs, prices, buy_fees, sell_fees, previous_w, setw):
        self.x = x
        self.price_incs = price_incs
        self.buy_fees = buy_fees
        self.sell_fees = sell_fees
        self.prices = prices
        self.previous_w = previous_w
        self.setw = setw


class DataMatrices:
    def __init__(self, database_dir, config):
        self.config = config
        self.__global_data = get_global_panel(database_dir, config)
        self.__PVM = pd.DataFrame(index=self.__global_data.minor_axis, columns=["BTC"] + config.coins)
        self.__PVM = self.__PVM.fillna(1.0 / (1 + config.coin_number))
        self.__divide_data(config.test_days, config.period)
        self.s = 0

    def get_test_set(self):
        return self.__pack_samples(self._test_ind)

    def train_batches(self):
        start_index = self._train_ind[0]
        end_index = self._train_ind[-1]
        experiences = range(start_index, end_index)
        end = len(experiences) - self.config.batch_size
        while True:
            batch_start = geometricSample(start_index, end, self.config.geometric_bias)
            indices = experiences[batch_start:batch_start + self.config.batch_size]
            yield self.__pack_samples(indices)

    def test_batches(self):
        start_index = self._test_ind[0]
        end_index = self._test_ind[-1]
        experiences = range(start_index, end_index)
        end = len(experiences)
        batch_start = 0
        while batch_start < end:
            indices = experiences[batch_start:min(batch_start + self.config.batch_size, end)]
            yield self.__pack_samples(indices)
            batch_start += self.config.batch_size

    def train_sequential_start(self):
        return self._train_ind[0] + self.config.batch_size * 2

    def train_sequential_end(self):
        return self._train_ind[-1]

    def train_batches_sequential(self, step, count):
        start_index = self._train_ind[0]
        end_index = step
        experiences = range(start_index, end_index)
        end = len(experiences) - self.config.batch_size
        for i in range(count):
            batch_start = geometricSample(start_index, end, self.config.train_sequential_bias)
            indices = experiences[batch_start:batch_start + self.config.batch_size]
            yield self.__pack_samples(indices)

    def test_batches_sequential(self, step):
        periods_per_day = 60 * 60 * 24 / self.config.period
        start_index = step
        end_index = step + self.config.test_days * periods_per_day
        experiences = range(start_index, end_index)
        end = len(experiences)
        batch_start = 0
        while batch_start < end:
            indices = experiences[batch_start:min(batch_start + self.config.batch_size, end)]
            yield self.__pack_samples(indices)
            batch_start += self.config.batch_size

    def __pack_samples(self, indexes):
        indexes = np.array(indexes)
        last_w = self.__PVM.values[indexes - 1, :]

        def get_submatrix(ind):
            return self.__global_data.values[:, :, ind:ind + self.config.window_size + 1]

        M = [get_submatrix(index) for index in indexes]
        M = np.array(M)
        bitcoin_M = np.ones((M.shape[0], M.shape[1], 1, M.shape[3]))
        # bitcoin_M_prices = np.ones((M.shape[0], M.shape[1] - 2, 1, M.shape[3]))
        # bitcoin_M_fees = np.zeros((M.shape[0], 2, 1, M.shape[3]))
        # bitcoin_M = np.concatenate((bitcoin_M_prices, bitcoin_M_fees), axis=1)
        M = np.concatenate((bitcoin_M, M), axis=2)
        x = M[:, :-1, :, :-1]
        prices = M[:, -1, :, -2]  # -3 indicator (second index) should be "z_price"
        price_incs = M[:, -1, :, -1] / M[:, -1, :, -2]

        # x = M[:, :-3, :, :-1]
        # prices = M[:, -3, :, -2]  # -3 indicator (second index) should be "z_price"
        # price_incs = M[:, -3, :, -1] / M[:, -3, :, -2]
        # buy_fees = M[:, -2, :, -2]  # -2 indicator (second index) should be "zz_buy_fee"
        # sell_fees = M[:, -1, :, -2]  # -1 indicator (second index) should be "zzz_sell_fee"

        def setw(w):
            self.__PVM.iloc[indexes, :] = w

        return DataBatch(x, price_incs, prices, None, None, last_w, setw)

    def __divide_data(self, test_days, period):
        periods_per_day = 24 * 60 * 60 / period
        test_periods = int(periods_per_day * test_days)
        num_periods = len(self.__global_data.minor_axis) - self.config.window_size - 1
        indices = np.arange(num_periods)
        self._train_ind, self._test_ind = np.split(indices, [num_periods - test_periods])
