import tflearn
import tensorflow as tf

#  todo solve problem with main coin

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
        nb_filter=4,
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
    def __init__(
            self,
            coin_number,
            history_size,
            indicator_number,
            gpu_memory_fraction,
            saved_file,
    ):
        self.coin_number = coin_number
        self.batch_count = tf.placeholder(tf.int32, shape=[])
        self.history = tf.placeholder(tf.float32, shape=[None, indicator_number,  coin_number - 1, history_size])       # without main coin (BTC)
        self.current_portfolio = tf.placeholder(tf.float32, shape=[None, coin_number - 1])      # without main coin (BTC)
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
             current_portfolio: batch_count x coin_number
             history: batch_count x coin_number x history_size x indicator_number
        """

        tflearn.is_training(False, self.session)
        result = self.session.run(self.best_portfolio, feed_dict={
            self.current_portfolio: current_portfolio[:, 1:],   # without main coin (BTC)
            self.history: history[:, 1:, :, :],   # without main coin (BTC)
            self.batch_count: history.shape[0]
        })
        return result

    def save(self, path):
        self.saver.save(self.session, path)

    def recycle(self):
        tf.reset_default_graph()
        self.session.close()


def compute_profits(batch_size, best_portfolio, future_price_incs, fees):
    pure_profits = future_price_incs * best_portfolio
    pure_profit = tf.reduce_sum(pure_profits, axis=1)
    future_w = pure_profits / pure_profit[:, None]

    w0 = future_w[:batch_size - 1]
    w1 = best_portfolio[1:batch_size]
    cost = 1 - tf.reduce_sum(tf.abs(w1 - w0) * fees, axis=1)  # w0 -> w1 commission for all steps except first step

    return pure_profit * tf.concat([tf.ones(1), cost], axis=0)


class NeuralTrainer:
    def __init__(
            self,
            network
    ):
        self.future_price_incs = tf.placeholder(tf.float32, shape=[None, network.coin_number - 1])        # without main coin (BTC)
        self.fees = tf.placeholder(tf.float32, shape=[None, network.coin_number])

        profits = compute_profits(network.batch_size, network.best_portfolio, self.future_price_incs, self.fees)
        capital = tf.reduce_prod(profits)
        self.geometric_mean_profit = tf.pow(capital, 1 / tf.to_float(network.batch_size))

        loss = -tf.reduce_mean(tf.log(profits))
        loss += tf.reduce_sum(tf.get_collection(tf.GraphKeys.REGULARIZATION_LOSSES))
        self.train = tf.train.AdamOptimizer(0.00028).minimize(loss)

        self.batch_size = network.batch_size
        self.history = network.history
        self.current_portfolio = network.current_portfolio
        self.best_portfolio = network.best_portfolio
        self.session = network.session

    def train(self, current_portfolio, history, future_price_incs, fees):
        """
           Args:
             current_portfolio: batch_count x coin_number
             history: batch_count x coin_number x history_size x indicator_number
             future_price_incs: batch_count x coin_number
             fees: batch_count x coin_number
        """
        tflearn.is_training(True, self.session)
        results = self.session.run([self.train, self.best_portfolio, self.geometric_mean_profit], feed_dict={
            self.current_portfolio: current_portfolio[:, 1:],   # without main coin (BTC)
            self.history: history[:, 1:, :, :],   # without main coin (BTC)
            self.future_price_incs: future_price_incs,
            self.fees: fees,
            self.batch_size: history.shape[0]
        })

        return results[1:]
