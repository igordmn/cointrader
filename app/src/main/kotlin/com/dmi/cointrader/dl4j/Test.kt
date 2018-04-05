package com.dmi.cointrader.dl4j

import org.deeplearning4j.rl4j.learning.async.nstep.discrete.AsyncNStepQLearningDiscrete
import org.deeplearning4j.rl4j.learning.async.nstep.discrete.AsyncNStepQLearningDiscreteDense
import org.deeplearning4j.rl4j.learning.sync.qlearning.QLearning
import org.deeplearning4j.rl4j.learning.sync.qlearning.discrete.QLearningDiscreteDense
import org.deeplearning4j.rl4j.mdp.toy.HardDeteministicToy
import org.deeplearning4j.rl4j.mdp.toy.SimpleToy
import org.deeplearning4j.rl4j.network.dqn.DQNFactoryStdDense
import org.deeplearning4j.rl4j.util.DataManager

var TOY_QL = QLearning.QLConfiguration(
        123, //Random seed
        100000, //Max step By epoch
        80000, //Max step
        10000, //Max size of experience replay
        32, //size of batches
        100, //target update (hard)
        0, //num step noop warmup
        0.05, //reward scaling
        0.99, //gamma
        10.0, //td-error clipping
        0.1f, //min epsilon
        2000, //num step for eps greedy anneal
        true   //double DQN
)


var TOY_ASYNC_QL = AsyncNStepQLearningDiscrete.AsyncNStepQLConfiguration(
        123, //Random seed
        100000, //Max step By epoch
        80000, //Max step
        8, //Number of threads
        5, //t_max
        100, //target update (hard)
        0, //num step noop warmup
        0.1, //reward scaling
        0.99, //gamma
        10.0, //td-error clipping
        0.1f, //min epsilon
        2000        //num step for eps greedy anneal
)


var TOY_NET: DQNFactoryStdDense.Configuration = DQNFactoryStdDense.Configuration.builder()
        .l2(0.01).learningRate(1e-2).numLayer(3).numHiddenNodes(16).build()

fun dl4jtest() {
    hardToy()
    //toyAsyncNstep()
}

fun simpleToy() {
    val manager = DataManager()
    val mdp = SimpleToy(20)
    val dql = QLearningDiscreteDense(mdp, TOY_NET, TOY_QL, manager)
    mdp.setFetchable(dql)
    dql.train()
    mdp.close()
}

fun hardToy() {
    val manager = DataManager()
    val mdp = HardDeteministicToy()
    val dql = QLearningDiscreteDense(mdp, TOY_NET, TOY_QL, manager)
    dql.train()
    mdp.close()
}

fun toyAsyncNstep() {
    val manager = DataManager()
    val mdp = SimpleToy(20)
    val dql = AsyncNStepQLearningDiscreteDense(mdp, TOY_NET, TOY_ASYNC_QL, manager)
    mdp.setFetchable(dql)
    dql.train()
    mdp.close()
}
