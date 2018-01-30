import numpy as np



batch_size = 4
fee = 0.1
price_inc = np.array([
    [1.1, 1.2, 1.3],
    [1.1, 1.2, 1.3],
    [1.1, 1.2, 1.3],
    [1.1, 1.2, 1.3]
])
predict_w = np.array([
    [0.3, 0.4, 0.3],
    [0.4, 0.3, 0.3],
    [0.3, 0.3, 0.4],
    [1.0, 0.0, 0.0]
])

pure_profits = price_inc * predict_w
print("pure_profits", pure_profits)
pure_profit = np.sum(pure_profits, axis=1)
print("pure_profit", pure_profit)
print("pure_profit[:, None]", pure_profit[:, None])

future_w = pure_profits / pure_profit[:, None]
print("future_w", future_w)
print("predict_w[0, None]", predict_w[0, None])
print("future_w[:batch_size - 1]", future_w[:batch_size - 1])
previous_w = np.concatenate([predict_w[0, None], future_w[:batch_size - 1]], axis=0)  # for first step assume portfolio equals predicted value
print("previous_w", previous_w)
cost = 1 - np.sum(np.abs(predict_w - previous_w), axis=1) * fee
print("np.sum(np.abs(predict_w - previous_w), axis=1)", np.sum(np.abs(predict_w - previous_w), axis=1))