from typing import NamedTuple

import tflearn
import tensorflow as tf



def eiie_dense(net, filter_number, activation_function, regularizer, weight_decay):
    width = net.get_shape()[2]
    return tflearn.layers.conv_2d(
        net, filter_number, [1, width], [1, 1], "valid", activation_function,
        regularizer=regularizer, weight_decay=weight_decay
    )


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

    # [batch, assets, window, features]
    net = net / net[:, :, -1, 0, None, None]  # divide on last close
    net = tf.log(net)

    # net = tflearn.layers.conv_2d(
    #     net,
    #     nb_filter=3,
    #     filter_size=[1, 2],
    #     strides=[1, 1],
    #     padding="valid",
    #     activation="relu",
    #     regularizer=None,
    #     weight_decay=0,
    # )
    # net = tflearn.batch_normalization(net)
    # net = eiie_dense(
    #     net,
    #     filter_number=10,
    #     activation_function="relu",
    #     regularizer="L2",
    #     weight_decay=5e-9,
    # )
    # net = tflearn.batch_normalization(net)
    #
    # net = eiie_output_withw(
    #     net,
    #     batch_size,
    #     previous_w,
    #     regularizer="L2",
    #     weight_decay=5e-8,
    # )

    net = tflearn.layers.conv_2d(
        net,
        nb_filter=config.conv_size,
        filter_size=[1, config.conv_kernel],
        strides=[1, 1],
        padding="valid",
        activation="relu",
        regularizer=None,
        weight_decay=config.weight_decay,
    )
    if config.use_batch_normalization:
        net = tflearn.batch_normalization(net)
    net = tflearn.dropout(net, config.dropout)

    net = tflearn.layers.conv_2d(
        net,
        nb_filter=config.conv_size,
        filter_size=[1, config.conv_kernel],
        strides=[1, 1],
        padding="valid",
        activation="relu",
        regularizer=None,
        weight_decay=config.weight_decay,
    )
    if config.use_batch_normalization:
        net = tflearn.batch_normalization(net)
    net = tflearn.dropout(net, config.dropout)

    net = eiie_dense(
        net,
        filter_number=config.dense_size,
        activation_function="relu",
        regularizer=None,
        weight_decay=config.weight_decay,
    )
    if config.use_batch_normalization:
        net = tflearn.batch_normalization(net)
    net = tflearn.dropout(net, config.dropout)

    net = eiie_dense(
        net,
        filter_number=config.dense_size,
        activation_function="relu",
        regularizer=None,
        weight_decay=config.weight_decay,
    )
    if config.use_batch_normalization:
        net = tflearn.batch_normalization(net)
    net = tflearn.dropout(net, config.dropout)

    net = eiie_output_withw(
        net,
        batch_size,
        previous_w,
        regularizer="L2",
        weight_decay=5e-8,
    )

    return net


def compute_profits(batch_size, previous_w, predict_w, price_inc, fee):
    future_portfolio = price_inc * predict_w
    future_w = future_portfolio / tf.reduce_sum(future_portfolio, axis=1)[:, None]

    w0 = future_w[:batch_size - 1]
    w1 = predict_w[1:batch_size]
    cost = 1 - tf.reduce_sum(tf.abs(w1 - w0), axis=1) * fee  # w0 -> w1 commission for all steps except first step

    return tf.reduce_sum(future_portfolio, axis=[1]) * tf.concat([tf.ones(1), cost], axis=0)


def compute_profits2(batch_size, previous_w, predict_w, price_inc, fee):
    future_portfolio = price_inc * predict_w
    future_commission = 1 - tf.reduce_sum(tf.abs(previous_w - predict_w), axis=1) * fee
    return tf.reduce_sum(future_portfolio, axis=[1]) * future_commission


class Tensors(NamedTuple):
    batch_size: tf.Tensor
    history: tf.Tensor
    price_incs: tf.Tensor
    previous_w: tf.Tensor
    predict_w: tf.Tensor

    capital: tf.Tensor
    geometric_mean_profit: tf.Tensor

    train: tf.Tensor


class NNConfig(NamedTuple):
    indicator_number: int
    coin_number: int
    window_size: int
    fee: int
    learning_rate: int
    weight_decay: float
    use_batch_normalization: bool
    dropout: float
    conv_size: int
    conv_kernel: int
    dense_size: int


def train_config_to_nn(config):
    return NNConfig(
        indicator_number=config.indicator_number,
        coin_number=config.coin_number + 1,   # with BTC
        window_size=config.window_size,
        fee=config.fee,
        learning_rate=config.learning_rate,
        weight_decay=config.weight_decay,
        use_batch_normalization=config.use_batch_normalization,
        dropout=config.dropout,
        conv_size=config.conv_size,
        conv_kernel=config.conv_kernel,
        dense_size=config.dense_size,
    )


class NNAgent:
    def __init__(
            self,
            config,
            restore_path=None,
    ):
        batch_size = tf.placeholder(tf.int32, shape=[])
        x = tf.placeholder(tf.float32, shape=[None, config.indicator_number,  config.coin_number, config.window_size])
        price_incs = tf.placeholder(tf.float32, shape=[None, config.coin_number])
        previous_w = tf.placeholder(tf.float32, shape=[None, config.coin_number])
        predict_w = build_predict_w(batch_size, config, x, previous_w)

        profits = compute_profits(batch_size, previous_w, predict_w, price_incs, config.fee)
        log_profits = tf.log(profits)
        capital = tf.reduce_prod(profits)
        geometric_mean = tf.pow(tf.reduce_prod(capital), 1 / tf.to_float(batch_size))
        log_mean = tf.reduce_mean(log_profits)

        loss = -log_mean
        loss += tf.reduce_sum(tf.get_collection(tf.GraphKeys.REGULARIZATION_LOSSES))
        train = tf.train.RMSPropOptimizer(config.learning_rate).minimize(loss)

        self._tensors = Tensors(
            batch_size,
            x,
            price_incs,
            previous_w,
            predict_w,

            capital,
            geometric_mean,

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

        tflearn.is_training(True, session)
        results = session.run([t.train, t.predict_w, t.geometric_mean_profit], feed_dict={
            t.history: batch.x,
            t.price_incs: batch.price_incs,
            t.previous_w: batch.previous_w,
            t.batch_size: batch.x.shape[0]
        })
        batch.setw(results[1])

        return results[2:]

    def trainNew(self, history, previous_w, price_incs):
        session = self._session
        t = self._tensors

        tflearn.is_training(True, session)
        results = session.run([t.train, t.predict_w, t.geometric_mean_profit], feed_dict={
            t.history: history,
            t.price_incs: price_incs,
            t.previous_w: previous_w,
            t.batch_size: history.shape[0]
        })

        return results[1:]

    def best_portfolio(self, history, previous_w):
        session = self._session
        t = self._tensors

        tflearn.is_training(False, session)

        result = session.run(t.predict_w, feed_dict={
            t.history: history,
            t.previous_w: previous_w,
            t.batch_size: history.shape[0]
        })

        return result

    def save(self, path):
        self._saver.save(self._session, path)
