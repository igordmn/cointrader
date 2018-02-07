from typing import NamedTuple

import tflearn
import tensorflow as tf
import numpy as np
import numpy


def compute_best_prices(history):
    net = history
    net = tf.log(net / net[:, -1, None, :])
    net = tflearn.layers.conv_1d(
        net,
        nb_filter=128,
        filter_size=3,
        padding='valid',
        activation='relu',
        regularizer="L2",
        weight_decay=5e-8
    )
    net = tflearn.fully_connected(
        net,
        128,
        activation='relu',
        regularizer="L2",
        weight_decay=5e-8
    )
    net = tflearn.fully_connected(
        net,
        128,
        activation='relu',
        regularizer="L2",
        weight_decay=5e-8
    )
    net = tflearn.fully_connected(
        net,
        1,
        activation='linear',
        regularizer="L2",
        weight_decay=5e-8
    )
    return net[:, :, 0]


def compute_capital_increase(buy_sell_high_low_prices):
    def next_capital(capital, buy_sell_high_low_price):
        buy = buy_sell_high_low_price[0]
        sell = buy_sell_high_low_price[1]
        high = buy_sell_high_low_price[2]
        low = buy_sell_high_low_price[3]

        need_buy = capital[0] > 0

        def capital_after_buy():
            return tf.constant([0, capital[0] / buy])

        def capital_after_sell():
            return tf.constant([capital[1] * sell, 0])

        def capital_after_buy_if_can():
            can_buy = low <= buy <= high
            return tf.cond(can_buy, lambda: capital_after_buy(), lambda: capital)

        def capital_after_sell_if_can():
            can_sell = low <= sell <= high
            return tf.constant(can_sell, lambda: capital_after_sell(), lambda: capital)

        return tf.cond(need_buy, lambda: capital_after_buy_if_can(), lambda: capital_after_sell_if_can())

    def capital_to_dollars(capital, buy_sell_high_low_price):
        is_dollars = capital[0] > 0
        low = buy_sell_high_low_price[3]
        return tf.cond(is_dollars, lambda: capital[0], lambda: capital[1] * low)

    initial_dollars = 10
    initial_capital = tf.constant([initial_dollars, 0])
    end_capital = tf.foldl(lambda c, x: next_capital(c, x), buy_sell_high_low_prices, initializer=initial_capital)
    end_dollars = capital_to_dollars(end_capital, buy_sell_high_low_prices[-1])
    return end_dollars / initial_dollars


class Tensors(NamedTuple):
    batch_size: tf.Tensor
    history: tf.Tensor
    close_prices: tf.Tensor
    next_high_prices: tf.Tensor
    next_low_prices: tf.Tensor
    best_buy_prices: tf.Tensor
    best_sell_prices: tf.Tensor
    log_capital_increase: tf.Tensor
    train: tf.Tensor


class NNAgentOrders:
    def __init__(
            self,
            indicator_number, history_size,
            restore_path=None,
    ):
        batch_size = tf.placeholder(tf.int32, shape=[])
        history = tf.placeholder(tf.float32, shape=[None, indicator_number, history_size])
        close_prices = tf.placeholder(tf.float32, shape=[None])
        next_high_prices = tf.placeholder(tf.float32, shape=[None])
        next_low_prices = tf.placeholder(tf.float32, shape=[None])
        best_buy_log_inc_prices = compute_best_prices(history)
        best_sell_log_inc_prices = compute_best_prices(history)
        best_buy_prices = close_prices * tf.exp(best_buy_log_inc_prices)
        best_sell_prices = close_prices * tf.exp(best_sell_log_inc_prices)
        buy_sell_high_low_prices = tf.stack([best_buy_prices, best_sell_prices, next_high_prices, next_low_prices], axis=1)

        capital_increase = compute_capital_increase(buy_sell_high_low_prices)
        log_capital_increase = tf.log(capital_increase)

        loss = -log_capital_increase
        loss += tf.reduce_sum(tf.get_collection(tf.GraphKeys.REGULARIZATION_LOSSES))
        train = tf.train.AdamOptimizer(0.00028 * 6).minimize(loss)

        self._tensors = Tensors(
            batch_size,
            history,
            close_prices,
            next_high_prices,
            next_low_prices,
            best_buy_prices,
            best_sell_prices,
            log_capital_increase,
            train
        )

        tf_config = tf.ConfigProto()
        tf_config.gpu_options.per_process_gpu_memory_fraction = 0.6
        self._session = tf.Session(config=tf_config)
        self._saver = tf.train.Saver()

        if restore_path:
            self._saver.restore(self._session, restore_path)
        else:
            self._session.run(tf.global_variables_initializer())

    def recycle(self):
        tf.reset_default_graph()
        self._session.close()

    def train(self, history, close_prices, next_high_prices, next_low_prices):
        session = self._session
        t = self._tensors

        tflearn.is_training(True, session)
        results = session.run([t.train, t.log_capital_increase], feed_dict={
            t.history: history,
            t.close_prices: close_prices,
            t.next_high_prices: next_high_prices,
            t.next_low_prices: next_low_prices,
            t.batch_size: history.shape[0]
        })

        return results[1:]

    def test(self, history, close_prices, next_high_prices, next_low_prices):
        session = self._session
        t = self._tensors

        tflearn.is_training(False, session)
        results = session.run(
            [
                t.log_capital_increase
            ],
            feed_dict={
                t.history: history,
                t.close_prices: close_prices,
                t.next_high_prices: next_high_prices,
                t.next_low_prices: next_low_prices,
                t.batch_size: history.shape[0]
            }
        )
        return results

    def best_buy_prices(self, history):
        session = self._session
        t = self._tensors

        tflearn.is_training(False, session)

        result = session.run(t.best_buy_prices, feed_dict={
            t.history: history,
            t.batch_size: history.shape[0]
        })

        return result

    def best_sell_prices(self, history):
        session = self._session
        t = self._tensors

        tflearn.is_training(False, session)

        result = session.run(t.best_sell_prices, feed_dict={
            t.history: history,
            t.batch_size: history.shape[0]
        })

        return result

    def save(self, path):
        self._saver.save(self._session, path)
