from typing import NamedTuple

import tflearn
import tensorflow as tf


def eiie_dense(net, filter_number, activation_function, regularizer, weight_decay):
    width = net.get_shape()[2]
    return tflearn.layers.conv_2d(
        net, filter_number, [1, width], [1, 1], "valid", activation_function,
        regularizer=regularizer, weight_decay=weight_decay
    )


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
        batch_size, history, previous_w
):
    net = tf.transpose(history, [0, 2, 3, 1])

    # [batch, assets, window, features]
    net = net / net[:, :, -1, 0, None, None]  # divide on last close
    net = tf.log(net)

    net = tflearn.layers.conv_2d(
        net,
        nb_filter=3,
        filter_size=[1, 2],
        strides=[1, 1],
        padding="valid",
        activation="relu",
        regularizer=None,
        weight_decay=0,
        weights_init='xavier'
    )
    net = tflearn.batch_normalization(net)

    net = eiie_dense(
        net,
        filter_number=8,
        activation_function="relu",
        regularizer="L2",
        weight_decay=5e-8
    )
    net = tflearn.batch_normalization(net)

    net = eiie_output_withw(
        net,
        batch_size,
        previous_w,
        regularizer="L2",
        weight_decay=5e-8
    )

    return net


def compute_profits(batch_size, predict_w, price_inc, fee):
    future_portfolio = price_inc * predict_w
    future_w = future_portfolio / tf.reduce_sum(future_portfolio, axis=1)[:, None]

    w0 = future_w[:batch_size - 1]
    w1 = predict_w[1:batch_size]
    cost = 1 - tf.reduce_sum(tf.abs(w1 - w0), axis=1) * fee  # w0 -> w1 commission for all steps except first step

    return tf.reduce_sum(future_portfolio, axis=[1]) * tf.concat([tf.ones(1), cost], axis=0)


class NeuralNetwork:
    def __init__(
            self,
            coin_count,
            history_count,
            indicator_count,
            gpu_memory_fraction,
            load_file,
    ):
        self.batch_size = tf.placeholder(tf.int32, shape=[])
        self.history = tf.placeholder(tf.float32, shape=[None, indicator_count,  coin_count, history_count])
        self.previous_w = tf.placeholder(tf.float32, shape=[None, coin_count])
        self.predict_w = build_predict_w(self.batch_size, self.history, self.previous_w)

        tf_config = tf.ConfigProto()
        tf_config.gpu_options.per_process_gpu_memory_fraction = gpu_memory_fraction
        self.session = tf.Session(config=tf_config)
        self.saver = tf.train.Saver()

        if load_file:
            self.saver.restore(self.session, load_file)
        else:
            self.session.run(tf.global_variables_initializer())

    def best_portfolio(self, history, previous_w):
        tflearn.is_training(False, self.session)

        result = self.session.run(self.predict_w, feed_dict={
            self.history: history,
            self.previous_w: previous_w,
            self.batch_size: history.shape[0]
        })

        return result

    def save(self, path):
        self.saver.save(self.session, path)

    def recycle(self):
        tf.reset_default_graph()
        self.session.close()


class NeuralTrainer:
    def __init__(
            self,
            fee,
            network
    ):
        self.price_incs = tf.placeholder(tf.float32, shape=[None, network.coin_number])

        self.profits = compute_profits(network.batch_size, network.predict_w, self.price_incs, fee)
        self.log_profits = tf.log(self.profits)
        self.capital = tf.reduce_prod(self.profits)
        self.geometric_mean_profit = tf.pow(tf.reduce_prod(self.capital), 1 / tf.to_float(network.batch_size))
        self.log_mean_profit = tf.reduce_mean(self.log_profits)

        self.loss = -self.log_mean_profit
        self.loss += tf.reduce_sum(tf.get_collection(tf.GraphKeys.REGULARIZATION_LOSSES))
        self.train = tf.train.RMSPropOptimizer(0.00028 * 2).minimize(self.loss)

        self.batch_size = network.batch_size
        self.history = network.history
        self.previous_w = network.previous_w
        self.predict_w = network.previous_w
        self.session = network.session

    def train(self, previous_w, history, price_incs):
        tflearn.is_training(True, self.session)
        results = self.session.run([self.train, self.predict_w, self.geometric_mean_profit], feed_dict={
            self.history: history,
            self.price_incs: price_incs,
            self.previous_w: previous_w,
            self.batch_size: history.shape[0]
        })

        return results[1:]
