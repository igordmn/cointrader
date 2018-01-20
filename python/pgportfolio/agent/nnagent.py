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


def eiie_output(net, batch_size, regularizer, weight_decay):
    width = net.get_shape()[2]
    net = tflearn.layers.conv_2d(
        net, 1, [1, width],
        padding="valid",
        regularizer=regularizer,
        weight_decay=weight_decay
    )
    net = net[:, :, 0, 0]
    btc_bias = tf.ones((batch_size, 1))
    net = tf.concat([btc_bias, net], 1)
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
    btc_bias = tf.get_variable("btc_bias", [1, 1], dtype=tf.float32, initializer=tf.zeros_initializer)
    btc_bias = tf.tile(btc_bias, [batch_size, 1])
    net = tf.concat([btc_bias, net], 1)
    return tflearn.layers.core.activation(net, activation="softmax")


def build_predict_w(
        batch_size, x, previous_w
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

    net = eiie_output_withw(
        net,
        batch_size,
        previous_w,
        regularizer="L2",
        weight_decay=5e-8,
    )

    return net


def build_loss(log_mean, sharp_ratio, predict_w):
    loss_tensor = -log_mean  # + 1.001 * tf.reduce_mean(tf.reduce_sum(-tf.log(1.000001 - predict_w), axis=[1]))
    loss_tensor += tf.reduce_sum(tf.get_collection(tf.GraphKeys.REGULARIZATION_LOSSES))
    return loss_tensor


def build_train(loss):
    return tf.train.AdamOptimizer(0.00028).minimize(loss)
    # global_step = tf.Variable(0, trainable=False)
    # learning_rate = tf.train.exponential_decay(0.0005, global_step, 5000, 0.75, staircase=True)
    # return tf.train.RMSPropOptimizer(learning_rate).minimize(loss, global_step=global_step)


def compute_profits(batch_size, predict_w, price_inc, fee):
    future_price = tf.concat([tf.ones([batch_size, 1]), price_inc], axis=1)
    future_portfolio = future_price * predict_w
    future_w = future_portfolio / tf.reduce_sum(future_portfolio, axis=1)[:, None]

    w0 = future_w[:batch_size - 1][:, 1:]
    w1 = predict_w[1:batch_size][:, 1:]
    future_commission = 1 - tf.reduce_sum(tf.abs(w1 - w0), axis=1) * fee  # w0 -> w1 commission for all steps except first step
    commission = tf.concat([tf.ones(1), future_commission], axis=0)

    return tf.reduce_sum(future_portfolio, axis=[1]) * commission


class Tensors(NamedTuple):
    batch_size: tf.Tensor
    x: tf.Tensor
    price_inc: tf.Tensor
    previous_w: tf.Tensor
    predict_w: tf.Tensor

    capital: tf.Tensor
    mean_profit: tf.Tensor
    geometric_mean_profit: tf.Tensor
    log_mean_profit: tf.Tensor
    standard_profit_deviation: tf.Tensor
    sharp_ratio: tf.Tensor
    loss: tf.Tensor
    train: tf.Tensor


class NNAgent:
    def __init__(
            self,
            fee, indicator_number, coin_number, window_size,
            restore_path=None,
    ):
        batch_size = tf.placeholder(tf.int32, shape=[])
        x = tf.placeholder(tf.float32, shape=[None, indicator_number, coin_number, window_size])
        price_inc = tf.placeholder(tf.float32, shape=[None, coin_number])
        previous_w = tf.placeholder(tf.float32, shape=[None, coin_number])
        predict_w = build_predict_w(batch_size, x, previous_w)

        profits = compute_profits(batch_size, predict_w, price_inc, fee)
        capital = tf.reduce_prod(profits)
        mean = tf.reduce_mean(profits)
        geometric_mean = tf.pow(tf.reduce_prod(capital), 1 / tf.to_float(batch_size))
        log_mean = tf.reduce_mean(tf.log(profits))
        standard_deviation = tf.sqrt(tf.reduce_mean((profits - mean) ** 2))
        sharp_ratio = (mean - 1) / standard_deviation
        loss = build_loss(log_mean, sharp_ratio, predict_w)

        train = build_train(loss)

        self._tensors = Tensors(
            batch_size,
            x,
            price_inc,
            previous_w,
            predict_w,

            capital,
            mean,
            geometric_mean,
            log_mean,
            standard_deviation,
            sharp_ratio,
            loss,
            train
        )

        tf_config = tf.ConfigProto()
        tf_config.gpu_options.per_process_gpu_memory_fraction = 0.2
        self._session = tf.Session(config=tf_config)
        self._saver = tf.train.Saver()

        if restore_path:
            self._saver.restore(self._session, restore_path)
        else:
            self._session.run(tf.global_variables_initializer())

    def recycle(self):
        tf.reset_default_graph()
        self._session.close()

    def train(self, x, price_inc, previous_w):
        session = self._session
        t = self._tensors

        tflearn.is_training(True, session)
        results = session.run([t.train, t.predict_w, t.geometric_mean_profit], feed_dict={
            t.x: x,
            t.price_inc: price_inc,
            t.previous_w: previous_w,
            t.batch_size: x.shape[0]
        })

        return results[1:]

    def test(self, x, price_inc, previous_w):
        session = self._session
        t = self._tensors

        tflearn.is_training(False, session)
        results = session.run(
            [t.capital, t.geometric_mean_profit, t.log_mean_profit, t.sharp_ratio, t.loss, t.predict_w],
            feed_dict={
                t.x: x,
                t.price_inc: price_inc,
                t.previous_w: previous_w,
                t.batch_size: x.shape[0]
            }
        )
        return results[:-1]

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