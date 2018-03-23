import tflearn
import tensorflow as tf


def eiie_dense(net, filter_number, activation_function, regularizer, weight_decay):
    width = net.get_shape()[2]
    return tflearn.layers.conv_2d(
        net, filter_number, [1, width], [1, 1], "valid", activation_function,
        regularizer=regularizer, weight_decay=weight_decay
    )


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
        weight_decay=weight_decay
    )
    net = net[:, :, 0, 0]
    main_coin_bias = tf.get_variable("main_coin_bias", [1, 1], dtype=tf.float32, initializer=tf.zeros_initializer)
    main_coin_bias = tf.tile(main_coin_bias, [batch_size, 1])
    net = tf.concat([main_coin_bias, net], 1)
    return tflearn.layers.core.activation(net, activation="softmax")


def build_best_portfolio(
        batch_size, history, current_portfolio
):
    # [batch, asset, history, indicator]
    net = history

    net = net / net[:, :, -1, 0, None, None]  # divide on last close
    net = tf.log(net)

    net = tflearn.layers.conv_2d(
        net,
        nb_filter=2,
        filter_size=[1, 2],
        strides=[1, 1],
        padding="valid",
        activation="leaky_relu",
        regularizer=None,
        weight_decay=0,
        weights_init='xavier'
    )
    net = tflearn.batch_normalization(net, decay=0.999)

    net = eiie_dense(
        net,
        filter_number=10,
        activation_function="leaky_relu",
        regularizer="L2",
        weight_decay=5e-9
    )
    net = tflearn.batch_normalization(net, decay=0.999)

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
        self.alt_asset_number = alt_asset_number
        self.batch_count = tf.placeholder(tf.int32, shape=[])
        self.history = tf.placeholder(tf.float32, shape=[None, history_indicator_number, alt_asset_number, history_size])
        self.current_portfolio = tf.placeholder(tf.float32, shape=[None, alt_asset_number])
        self.best_portfolio = build_best_portfolio(self.batch_count, self.history, self.current_portfolio)

        tf_config = tf.ConfigProto()
        tf_config.gpu_options.per_process_gpu_memory_fraction = gpu_memory_fraction
        self.session = tf.Session(config=tf_config)
        self.saver = tf.train.Saver()

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
        result = self.session.run(self.best_portfolio, feed_dict={
            self.current_portfolio: current_portfolio,
            self.history: history,
            self.batch_count: history.shape[0]
        })
        return result

    def save(self, path):
        self.saver.save(self.session, path)

    def recycle(self):
        tf.reset_default_graph()
        self.session.close()


def compute_profits(best_portfolio, asks, bids, fee):
    asks = tf.concat([tf.ones([asks.shape[0], 1]), asks], axis=1)  # add main asset price
    bids = tf.concat([tf.ones([bids.shape[0], 1]), bids], axis=1)  # add main asset price
    prices = (asks + bids) / 2.0
    costs = (1.0 - fee) * (bids / prices)
    
    price_incs = prices[1:] / prices[:-1]
    costs = costs[:-1]
    best_portfolio = best_portfolio[:-1]
    future_portfolio = price_incs * best_portfolio / tf.reduce_sum(price_incs * best_portfolio, axis=1)[:, None]
    
    price_incs = price_incs[1:]
    costs = costs[1:]
    best_portfolio = best_portfolio[1:]
    current_portfolio = future_portfolio[:-1]
    cost = 1.0 - tf.reduce_sum(tf.abs(best_portfolio[:, 1:] - current_portfolio[:, 1:]) * costs, axis=1)

    return price_incs * best_portfolio * cost


class NeuralTrainer:
    def __init__(self, network, fee):
        self.asks = tf.placeholder(tf.float32, shape=[None, network.alt_asset_number])
        self.bids = tf.placeholder(tf.float32, shape=[None, network.alt_asset_number])

        profits = compute_profits(network.best_portfolio, self.asks, self.bids, fee)
        self.geometric_mean_profit = tf.pow(tf.reduce_prod(profits), 1.0 / profits.shape[0])

        loss = -tf.reduce_mean(tf.log(profits))
        loss += tf.reduce_sum(tf.get_collection(tf.GraphKeys.REGULARIZATION_LOSSES))
        self.train = tf.train.AdamOptimizer(0.00028).minimize(loss)

        self.batch_size = network.batch_size
        self.history = network.history
        self.current_portfolio = network.current_portfolio
        self.best_portfolio = network.best_portfolio
        self.session = network.session

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
        results = self.session.run([self.train, self.best_portfolio, self.geometric_mean_profit], feed_dict={
            self.current_portfolio: current_portfolio,
            self.history: history,
            self.asks: asks,
            self.bids: bids,
            self.batch_size: history.shape[0]
        })

        return results[1:]
