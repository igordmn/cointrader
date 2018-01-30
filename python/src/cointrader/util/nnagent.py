from typing import NamedTuple

import tflearn
import tensorflow as tf
import numpy as np


def eiie_dense(net, filter_number, activation_function, regularizer, weight_decay):
    width = net.get_shape()[2]
    return tflearn.layers.conv_2d(
        net, filter_number, [1, width], [1, 1], "valid", activation_function,
        regularizer=regularizer, weight_decay=weight_decay
    )


def eiie_lstm(net, coin_number):
    neuron_number = 256
    dropout = 0.2
    net = tf.transpose(net, [0, 2, 3, 1])

    resultlist = []
    for i in range(coin_number):
        result = tflearn.layers.lstm(net[:, :, :, i], neuron_number, dropout=dropout, scope="eiie_lstm", reuse=i > 0)
        resultlist.append(result)

    net = tf.stack(resultlist)
    net = tf.transpose(net, [1, 0, 2])
    return tf.reshape(net, [-1, coin_number, 1, neuron_number])


def eiie_output(net, regularizer, weight_decay):
    width = net.get_shape()[2]
    net = tflearn.layers.conv_2d(
        net, 1, [1, width],
        padding="valid",
        regularizer=regularizer,
        weight_decay=weight_decay
    )
    net = net[:, :, 0, 0]
    return tflearn.layers.core.activation(net, activation="softmax")


def eiie_output_withw(net, batch_size, previous_w, regularizer, weight_decay):
    width = net.get_shape()[2]
    height = net.get_shape()[1]
    features = net.get_shape()[3]
    net = tf.reshape(net, [batch_size, int(height), 1, int(width * features)])
    w = tf.reshape(previous_w, [-1, int(height), 1, 1])
    net = tf.concat([net, w], axis=3)
    net = tflearn.layers.conv_2d(
        net, 1, [1, 1],
        padding="valid",
        regularizer=regularizer,
        weight_decay=weight_decay
    )
    net = net[:, :, 0, 0]
    return tflearn.layers.core.activation(net, activation="softmax")


def build_predict_w(
        batch_size, coin_number, x, previous_w
):
    net = tf.transpose(x, [0, 2, 3, 1])
    net = net / net[:, :, -1, 0, None, None]
    net = tflearn.layers.conv_2d(
        net,
        nb_filter=3,
        filter_size=[1, 2],
        strides=[1, 1],
        padding="valid",
        activation="relu",
        regularizer=None,
        weight_decay=0,
    )
    net = eiie_dense(
        net,
        filter_number=10,
        activation_function="relu",
        regularizer="L2",
        weight_decay=5e-9,
    )

    # net = eiie_lstm(net, coin_number)

    net = eiie_output_withw(
        net,
        batch_size,
        previous_w,
        regularizer="L2",
        weight_decay=5e-8,
    )

    return net


def compute_profits(batch_size, predict_w, price_incs, buy_fees, sell_fees):
    pure_profits = price_incs * predict_w
    pure_profit = tf.reduce_sum(pure_profits, axis=1)

    future_w = pure_profits / pure_profit[:, None]
    previous_w = tf.concat([predict_w[0, None], future_w[:batch_size - 1]], axis=0)  # for first step assume portfolio equals predicted value
    diffs = predict_w - previous_w
    buys = tf.nn.relu(diffs)
    sells = tf.nn.relu(-diffs)
    total_fee = tf.reduce_sum(buys * buy_fees, axis=1) + tf.reduce_sum(sells * sell_fees, axis=1)

    return pure_profit * (1 - total_fee)


class Tensors(NamedTuple):
    batch_size: tf.Tensor
    x: tf.Tensor
    price_incs: tf.Tensor
    buy_fees: tf.Tensor
    sell_fees: tf.Tensor
    previous_w: tf.Tensor
    predict_w: tf.Tensor

    capital: tf.Tensor
    geometric_mean_profit: tf.Tensor
    log_mean_profit: tf.Tensor
    standard_profit_deviation: tf.Tensor
    downside_profit_deviation: tf.Tensor
    sharp_ratio: tf.Tensor
    sortino_ratio: tf.Tensor

    train: tf.Tensor


class NNAgent:
    def __init__(
            self,
            indicator_number, coin_number, window_size,
            restore_path=None,
    ):
        batch_size = tf.placeholder(tf.int32, shape=[])
        x = tf.placeholder(tf.float32, shape=[None, indicator_number, coin_number, window_size])
        price_incs = tf.placeholder(tf.float32, shape=[None, coin_number])
        buy_fees = tf.placeholder(tf.float32, shape=[None, coin_number])
        sell_fees = tf.placeholder(tf.float32, shape=[None, coin_number])
        previous_w = tf.placeholder(tf.float32, shape=[None, coin_number])
        predict_w = build_predict_w(batch_size, coin_number, x, previous_w)

        profits = compute_profits(batch_size, predict_w, price_incs, buy_fees, sell_fees)
        log_profits = tf.log(profits)
        capital = tf.reduce_prod(profits)
        geometric_mean = tf.pow(tf.reduce_prod(capital), 1 / tf.to_float(batch_size))
        log_mean = tf.reduce_mean(log_profits)

        standard_deviation = tf.sqrt(tf.reduce_mean((log_profits - log_mean) ** 2))
        downside_deviation = tf.sqrt(tf.reduce_mean(tf.minimum(0.0, log_profits) ** 2))
        sharp_ratio = log_mean / standard_deviation
        sortino_ratio = log_mean / downside_deviation

        loss = -log_mean
        loss += tf.reduce_sum(tf.get_collection(tf.GraphKeys.REGULARIZATION_LOSSES))
        train = tf.train.AdamOptimizer(0.00028).minimize(loss)

        self._tensors = Tensors(
            batch_size,
            x,
            price_incs,
            buy_fees,
            sell_fees,
            previous_w,
            predict_w,

            capital,
            geometric_mean,
            log_mean,
            standard_deviation,
            downside_deviation,
            sharp_ratio,
            sortino_ratio,

            train
        )

        tf_config = tf.ConfigProto()
        tf_config.gpu_options.per_process_gpu_memory_fraction = 0.4
        self._session = tf.Session(config=tf_config)
        self._saver = tf.train.Saver()

        if restore_path:
            self._saver.restore(self._session, restore_path)
        else:
            self._session.run(tf.global_variables_initializer())

    def recycle(self):
        tf.reset_default_graph()
        self._session.close()

    def train(self, x, price_incs, buy_fees, sell_fees, previous_w):
        session = self._session
        t = self._tensors

        tflearn.is_training(True, session)
        results = session.run([t.train, t.predict_w, t.geometric_mean_profit], feed_dict={
            t.x: x,
            t.price_incs: price_incs,
            t.buy_fees: buy_fees,
            t.sell_fees: sell_fees,
            t.previous_w: previous_w,
            t.batch_size: x.shape[0]
        })

        return results[1:]

    def test(self, x, price_incs, buy_fees, sell_fees, previous_w):
        session = self._session
        t = self._tensors

        tflearn.is_training(False, session)
        results = session.run(
            [
                t.capital, t.geometric_mean_profit, t.log_mean_profit,
                t.sharp_ratio, t.sortino_ratio,
                t.standard_profit_deviation, t.downside_profit_deviation
            ],
            feed_dict={
                t.x: x,
                t.price_incs: price_incs,
                t.buy_fees: buy_fees,
                t.sell_fees: sell_fees,
                t.previous_w: previous_w,
                t.batch_size: x.shape[0]
            }
        )
        return results

    def best_portfolio(self, history, previous_w):
        session = self._session
        t = self._tensors

        tflearn.is_training(False, session)

        result = session.run(t.predict_w, feed_dict={
            t.x: history,
            t.previous_w: previous_w,
            t.batch_size: history.shape[0]
        })

        return result

    def save(self, path):
        self._saver.save(self._session, path)
