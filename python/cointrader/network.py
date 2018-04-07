import tflearn
import tensorflow as tf

import numpy as np
from tensorflow.contrib.layers import batch_norm
from tensorflow.python.framework import ops
from tensorflow.python.ops import math_ops

from cointrader.amsgrad import AMSGrad


def lstm(net, alt_asset_number):
    neuron_number = 10
    net = tf.transpose(net, [0, 2, 3, 1])
    resultlist = []
    for i in range(alt_asset_number):
        resultlist.append(tflearn.layers.lstm(net[:, :, :, i], neuron_number, dropout=0.5, scope="lstm1", reuse=i > 0))
    net = tf.stack(resultlist)
    net = tf.transpose(net, [1, 0, 2])
    return tf.reshape(net, [-1, alt_asset_number, 1, neuron_number])


def eiie_dense(net, filter_number, activation_function, regularizer, weight_decay):
    width = net.get_shape()[2]
    return tflearn.layers.conv_2d(
        net, filter_number, [1, width], [1, 1], "valid", activation_function,
        regularizer=regularizer, weight_decay=weight_decay,
        weights_init='xavier'
    )


def eiie_output(net, batch_size, previous_portfolio, regularizer, weight_decay):
    width = net.get_shape()[2]
    net = tflearn.layers.conv_2d(
        net, 1, [1, width],
        padding="valid",
        regularizer=regularizer,
        weight_decay=weight_decay,
        weights_init='xavier'
    )
    net = net[:, :, 0, 0]
    main_asset_bias = tf.get_variable("main_asset_bias", [1, 1], dtype=tf.float32, initializer=tf.zeros_initializer)
    main_asset_bias = tf.tile(main_asset_bias, [batch_size, 1])
    net = tf.concat([main_asset_bias, net], 1)
    return tflearn.layers.core.activation(net, activation="softmax")


def eiie_output_withw(net, batch_size, previous_portfolio, regularizer, weight_decay):
    width = net.get_shape()[2]
    height = net.get_shape()[1]
    features = net.get_shape()[3]
    net = tf.reshape(net, [batch_size, int(height), 1, int(width * features)])
    w = tf.reshape(previous_portfolio, [-1, int(height), 1, 1])
    net = tf.concat([net, w], axis=3)
    net = tflearn.layers.conv_2d(
        net, 1, [1, 1],
        padding="valid",
        regularizer=regularizer,
        weight_decay=weight_decay,
        weights_init='xavier'
    )
    net = net[:, :, 0, 0]
    main_asset_bias = tf.get_variable("main_asset_bias", [1, 1], dtype=tf.float32, initializer=tf.zeros_initializer)
    main_asset_bias = tf.tile(main_asset_bias, [batch_size, 1])
    net = tf.concat([main_asset_bias, net], 1)
    return tflearn.layers.core.activation(net, activation="softmax")


def build_best_portfolio(
        batch_size, alt_asset_number, history, current_portfolio
):
    # [batch, asset, history, indicator]
    net = history

    net = net / net[:, :, -1, 0, None, None]  # divide on last ask
    net = tf.log(net)

    net = tflearn.layers.conv_2d(
        net,
        nb_filter=6,
        filter_size=[1, 5],
        strides=[1, 1],
        padding="valid",
        activation="relu",
        regularizer="L2",
        weight_decay=5e-9,
        weights_init='xavier'
    )
    # net = batch_norm(net, is_training=tflearn.get_training_mode(), decay=0.99)
    # net = batch_norm(net, is_training=tflearn.get_training_mode(), decay=0.999)
    # net = batch_norm(net, is_training=tflearn.get_training_mode(), renorm=True, renorm_decay=0.9, decay=0.9,)
    # net = tflearn.batch_normalization(net, decay=0.999)
    # net = tflearn.activations.relu(net)

    net = eiie_dense(
        net,
        filter_number=20,
        activation_function="relu",
        regularizer="L2",
        weight_decay=5e-9
    )
    # net = batch_norm(net, is_training=tflearn.get_training_mode(), renorm=True, renorm_decay=0.9, decay=0.9,)
    # net = tflearn.batch_normalization(net, decay=0.999)
    # net = tflearn.activations.relu(net)

    net = eiie_output_withw(
        net,
        batch_size,
        current_portfolio,
        regularizer="L2",
        weight_decay=5e-8
    )

    return net


class NeuralNetwork:
    def __init__(self, alt_asset_number, history_size, history_indicator_number, gpu_memory_fraction, saved_file):
        tflearn.config.init_training_mode()
        self.alt_asset_number = alt_asset_number
        self.batch_size = tf.placeholder(tf.int32, shape=[])
        self.history = tf.placeholder(tf.float32, shape=[None, alt_asset_number, history_size, history_indicator_number])
        self.current_portfolio = tf.placeholder(tf.float32, shape=[None, alt_asset_number])
        self.best_portfolio_tensor = build_best_portfolio(self.batch_size, self.alt_asset_number, self.history, self.current_portfolio)

        tf_config = tf.ConfigProto()
        tf_config.gpu_options.per_process_gpu_memory_fraction = gpu_memory_fraction
        self.session = tf.Session(config=tf_config)
        self.saver = tf.train.Saver(max_to_keep=None)

        if saved_file:
            self.saver.restore(self.session, saved_file)
        else:
            self.session.run(tf.global_variables_initializer())

    def best_portfolio(self, current_portfolio, history):
        """
            Args:
                current_portfolio: batch_count x alt_asset_number
                history: batch_count x alt_asset_number x history_size x history_indicator_number

            Returns:
                best_portfolio: batch_count x (1 + alt_asset_number)
        """

        tflearn.is_training(False, self.session)
        result = self.session.run(self.best_portfolio_tensor, feed_dict={
            self.current_portfolio: current_portfolio,
            self.history: history,
            self.batch_size: history.shape[0]
        })
        return result

    def save(self, path):
        self.saver.save(self.session, path)

    def recycle(self):
        tf.reset_default_graph()
        self.session.close()


def compute_profits(batch_size, best_portfolio, asks, bids, fee):
    asks = tf.concat([tf.ones([batch_size, 1]), asks], axis=1)  # add main asset price
    bids = tf.concat([tf.ones([batch_size, 1]), bids], axis=1)  # add main asset price
    prices = (asks + bids) / 2.0
    fees = 1 - (1.0 - fee) * (bids / prices)

    price_incs = prices[1:] / prices[:-1]
    fees = fees[:-1]
    best_portfolio = best_portfolio[:-1]
    future_portfolio = price_incs * best_portfolio / tf.reduce_sum(price_incs * best_portfolio, axis=1)[:, None]
    
    price_incs = price_incs[1:]
    fees = fees[1:]
    best_portfolio = best_portfolio[1:]
    current_portfolio = future_portfolio[:-1]
    cost = 1.0 - tf.reduce_sum(tf.abs(best_portfolio[:, 1:] - current_portfolio[:, 1:]) * fees[:, 1:], axis=1)
    profit = tf.reduce_sum(price_incs * best_portfolio, axis=1)

    return batch_size - 2, profit * cost


def clr(global_step, base_lr=0.00004, max_lr=0.00028 * 2, step_size=5000., decay=0.8):
    global_step = math_ops.cast(global_step, tf.float32)
    cycle = tf.floor(1 + global_step / (2 * step_size))
    x = tf.abs(global_step / step_size - 2 * cycle + 1)
    return base_lr + (max_lr - base_lr) * tf.maximum(0.0, (1 - x)) * (decay ** cycle)


class NeuralTrainer:
    def __init__(self, network, fee):
        self.asks = tf.placeholder(tf.float32, shape=[None, network.alt_asset_number])
        self.bids = tf.placeholder(tf.float32, shape=[None, network.alt_asset_number])

        profits_size, profits = compute_profits(network.batch_size, network.best_portfolio_tensor, self.asks, self.bids, fee)
        self.geometric_mean_profit = tf.pow(tf.reduce_prod(profits), 1.0 / tf.to_float(profits_size))

        loss = -tf.reduce_mean(tf.log(profits))
        loss += tf.reduce_sum(tf.get_collection(tf.GraphKeys.REGULARIZATION_LOSSES))

        global_step = tf.Variable(0, trainable=False)
        learning_rate = clr(global_step)
        self.train_tensor = tf.train.AdamOptimizer(learning_rate).minimize(loss, global_step=global_step)
        # self.train_tensor = tf.train.AdamOptimizer(learning_rate, epsilon=1e-9).minimize(loss, global_step=global_step)
        # self.train_tensor = tf.train.AdamOptimizer(learning_rate, beta2=0.9999).minimize(loss, global_step=global_step)
        # self.train_tensor = tf.train.AdamOptimizer(learning_rate, beta2=0.9).minimize(loss, global_step=global_step)
        # self.train_tensor = AMSGrad(learning_rate).minimize(loss, global_step=global_step)

        self.batch_size = network.batch_size
        self.history = network.history
        self.current_portfolio = network.current_portfolio
        self.best_portfolio_tensor = network.best_portfolio_tensor
        self.session = network.session
        self.session.run(tf.global_variables_initializer())

    def train(self, current_portfolio, history, asks, bids):
        """
            Args:
                current_portfolio: batch_count x alt_asset_number
                history: batch_count x alt_asset_number x history_size x history_indicator_number
                asks: batch_count x alt_asset_number
                bids: batch_count x alt_asset_number

            Returns:
                best_portfolio: batch_count x (1 + alt_asset_number)
                geometric_mean_profit
        """
        tflearn.is_training(True, self.session)
        results = self.session.run([self.train_tensor, self.best_portfolio_tensor, self.geometric_mean_profit], feed_dict={
            self.current_portfolio: current_portfolio,
            self.history: history,
            self.asks: asks,
            self.bids: bids,
            self.batch_size: history.shape[0]
        })

        return results[1], float(results[2])
