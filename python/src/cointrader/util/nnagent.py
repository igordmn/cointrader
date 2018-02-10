from typing import NamedTuple

import tflearn
import tensorflow as tf
import numpy as np
import numpy


def eiie_dense(net, filter_number, activation_function, regularizer, weight_decay):
    width = net.get_shape()[2]
    return tflearn.layers.conv_2d(
        net, filter_number, [1, width], [1, 1], "valid", activation_function,
        regularizer=regularizer, weight_decay=weight_decay
    )


def eiie_lstm(net, coin_number):
    neuron_number = 128
    dropout = 0.1
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
        batch_size, config, x, previous_w
):
    net = tf.transpose(x, [0, 2, 3, 1])
    net = tf.log(net / net[:, :, -1, None, :])
    net = tflearn.layers.conv_2d(
        net,
        nb_filter=config.conv_size,
        filter_size=[1, config.conv_kernel],
        strides=[1, 1],
        padding="valid",
        activation="relu",
        regularizer="L2",
        weight_decay=config.weight_decay,
    )
    # net = tflearn.batch_normalization(net)
    # net = tflearn.dropout(net, 0.2)
    # net = tflearn.layers.conv.max_pool_2d(net, [1, 2])
    # net = tflearn.layers.conv_2d(
    #     net,
    #     nb_filter=16,
    #     filter_size=[1, 2],
    #     strides=[1, 1],
    #     padding="valid",
    #     activation="relu",
    #     regularizer="L2",
    #     weight_decay=5e-8,
    # )
    # net = tflearn.layers.conv.max_pool_2d(net, [1, 2])
    # net = tflearn.layers.conv_2d(
    #     net,
    #     nb_filter=32,
    #     filter_size=[1, 2],
    #     strides=[1, 1],
    #     padding="valid",
    #     activation="relu",
    #     regularizer="L2",
    #     weight_decay=5e-8,
    # )
    # net = tflearn.layers.conv.max_pool_2d(net, [1, 2])
    # net = tflearn.layers.conv_2d(
    #     net,
    #     nb_filter=64,
    #     filter_size=[1, 2],
    #     strides=[1, 1],
    #     padding="valid",
    #     activation="relu",
    #     regularizer="L2",
    #     weight_decay=5e-8,
    # )
    # net = tflearn.layers.conv.max_pool_2d(net, [1, 2])
    net = eiie_dense(
        net,
        filter_number=config.dense_size,
        activation_function="relu",
        regularizer="L2",
        weight_decay=config.weight_decay,
    )
    # net = tflearn.batch_normalization(net)
    # net = tflearn.dropout(net, 0.2)
    net = eiie_dense(
        net,
        filter_number=config.dense_size,
        activation_function="relu",
        regularizer="L2",
        weight_decay=config.weight_decay,
    )
    # net = tflearn.batch_normalization(net)
    # net = tflearn.dropout(net, 0.2)

    # net = eiie_lstm(net, coin_number)

    # net = eiie_output(
    #     net,
    #     regularizer="L2",
    #     weight_decay=5e-5,
    # )

    net = eiie_output_withw(
        net,
        batch_size,
        previous_w,
        regularizer="L2",
        weight_decay=config.weight_decay,
    )

    return net


def compute_profits2(batch_size, previous_w, predict_w, price_incs, buy_fees, sell_fees):
    pure_profits = price_incs * predict_w
    pure_profit = tf.reduce_sum(pure_profits, axis=1)

    future_w = pure_profits / pure_profit[:, None]
    previous_w = tf.concat([predict_w[0, None], future_w[:batch_size - 1]],
                           axis=0)  # for first step assume portfolio equals predicted value
    diffs = predict_w - previous_w
    buys = tf.nn.relu(diffs)
    sells = tf.nn.relu(-diffs)
    total_fee = tf.reduce_sum(buys * buy_fees, axis=1) + tf.reduce_sum(sells * sell_fees, axis=1)

    return pure_profit * (1 - total_fee)


def compute_profits(batch_size, previous_w, predict_w, price_inc, fee):
    future_portfolio = price_inc * predict_w
    future_w = future_portfolio / tf.reduce_sum(future_portfolio, axis=1)[:, None]

    w0 = future_w[:batch_size - 1]
    w1 = predict_w[1:batch_size]
    future_commission = 1 - tf.reduce_sum(tf.abs(w1 - w0), axis=1) * fee  # w0 -> w1 commission for all steps except first step

    return tf.reduce_sum(future_portfolio, axis=[1]) * tf.concat([tf.ones(1), future_commission], axis=0)


def compute_profits3(batch_size, previous_w, predict_w, price_inc, fee):
    future_portfolio = price_inc * predict_w
    future_commission = 1 - tf.reduce_sum(tf.abs(previous_w - predict_w), axis=1) * fee
    return tf.reduce_sum(future_portfolio, axis=[1]) * future_commission


def compute_profits_fix(batch_size, previous_w, predict_w, price_inc, fee):
    future_portfolio = price_inc * predict_w
    future_w = future_portfolio / tf.reduce_sum(future_portfolio, axis=1)[:, None]

    w0 = future_w[:batch_size - 1]
    w1 = predict_w[1:batch_size]
    future_commission = tf.reduce_sum(tf.abs(w1 - w0), axis=1) * fee  # w0 -> w1 commission for all steps except first step
    return tf.reduce_sum(future_portfolio, axis=[1]) - tf.concat([tf.zeros(1), future_commission], axis=0)


def compute_profits3_fix(batch_size, previous_w, predict_w, price_inc, fee):
    future_portfolio = price_inc * predict_w
    return tf.reduce_sum(future_portfolio, axis=[1]) - tf.reduce_sum(tf.abs(previous_w - predict_w), axis=1) * fee



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
            config,
            restore_path=None,
    ):
        batch_size = tf.placeholder(tf.int32, shape=[])
        indicator_number = len(config.indicators)
        coin_number = 1 + len(config.coins)  # with BTC
        x = tf.placeholder(tf.float32, shape=[None, indicator_number, coin_number, config.window_size])
        price_incs = tf.placeholder(tf.float32, shape=[None, coin_number])
        buy_fees = tf.placeholder(tf.float32, shape=[None, coin_number])
        sell_fees = tf.placeholder(tf.float32, shape=[None, coin_number])
        previous_w = tf.placeholder(tf.float32, shape=[None, coin_number])
        predict_w = build_predict_w(batch_size, config, x, previous_w)

        # profits = compute_profits(batch_size, previous_w, predict_w, price_incs, buy_fees, sell_fees)
        profits = compute_profits_fix(batch_size, previous_w, predict_w, price_incs, config.fee)
        # profits = compute_profits(batch_size, previous_w, predict_w, price_incs, fee)
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
        train = tf.train.AdamOptimizer(config.learning_rate).minimize(loss)

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
        tf_config.gpu_options.per_process_gpu_memory_fraction = 0.5
        self._session = tf.Session(config=tf_config)
        self._saver = tf.train.Saver()

        if restore_path:
            self._saver.restore(self._session, restore_path)
        else:
            self._session.run(tf.global_variables_initializer())

    def recycle(self):
        tf.reset_default_graph()
        self._session.close()

    def train(self, batch):
        session = self._session
        t = self._tensors

        # indices = np.random.permutation(x_old.shape[2])
        # x = np.take(x_old, indices, axis=2)
        # price_incs = np.take(price_incs_old, indices, axis=1)
        # buy_fees = np.take(buy_fees_old, indices, axis=1)
        # sell_fees = np.take(sell_fees_old, indices, axis=1)
        # previous_w = np.take(previous_w_old, indices, axis=1)

        tflearn.is_training(True, session)
        results = session.run([t.train, t.predict_w, t.geometric_mean_profit], feed_dict={
            t.x: batch.x,
            t.price_incs: batch.price_incs,
            # t.buy_fees: batch.buy_fees,
            # t.sell_fees: batch.sell_fees,
            t.previous_w: batch.previous_w,
            t.batch_size: batch.x.shape[0]
        })
        batch.setw(results[1])

        return results[2:]

    def test(self, batch):
        session = self._session
        t = self._tensors

        tflearn.is_training(False, session)
        results = session.run(
            [
                t.predict_w, t.geometric_mean_profit
            ],
            feed_dict={
                t.x: batch.x,
                t.price_incs: batch.price_incs,
                # t.buy_fees: batch.buy_fees,
                # t.sell_fees: batch.sell_fees,
                t.previous_w: batch.previous_w,
                t.batch_size: batch.x.shape[0]
            }
        )
        batch.setw(results[0])
        return results[1:]

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
