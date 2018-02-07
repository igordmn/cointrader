import tensorflow as tf

from src.cointrader.withorders.nnagent_orders import NNAgentOrders

history_size = 320
path = "D:/Development/Projects/cointrader/gdaxBTCUSD.csv"

agent = NNAgentOrders(3, history_size)
