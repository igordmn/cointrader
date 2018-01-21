import numpy as np
import matplotlib.pyplot as plt


def plot_log(capitals, config):
    capitals = np.array(capitals)
    fig, ax = plt.subplots()
    dates = np.array(range(0, len(capitals))) * config.period / 3600 / 24
    ax.plot(dates, capitals)
    ax.set_yscale('log')
    ax.set(xlabel='date', ylabel='capital')
    ax.grid()
    plt.show()