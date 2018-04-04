package com.dmi.cointrader.dl4j

import java.io.IOException
import org.deeplearning4j.rl4j.learning.ILearning
import org.deeplearning4j.rl4j.learning.Learning
import org.deeplearning4j.rl4j.learning.async.nstep.discrete.AsyncNStepQLearningDiscrete
import org.deeplearning4j.rl4j.learning.async.nstep.discrete.AsyncNStepQLearningDiscreteDense
import org.deeplearning4j.rl4j.learning.sync.qlearning.QLearning
import org.deeplearning4j.rl4j.learning.sync.qlearning.discrete.QLearningDiscreteDense
import org.deeplearning4j.rl4j.mdp.MDP
import org.deeplearning4j.rl4j.mdp.toy.HardDeteministicToy
import org.deeplearning4j.rl4j.mdp.toy.SimpleToy
import org.deeplearning4j.rl4j.mdp.toy.SimpleToyState
import org.deeplearning4j.rl4j.network.dqn.DQNFactoryStdDense
import org.deeplearning4j.rl4j.network.dqn.IDQN
import org.deeplearning4j.rl4j.space.DiscreteSpace
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
    //record the training data in rl4j-data in a new folder
    val manager = DataManager()

    //define the mdp from toy (toy length)
    val mdp = SimpleToy(20)

    //define the training method
    val dql = QLearningDiscreteDense(mdp, TOY_NET, TOY_QL, manager)

    //enable some logging for debug purposes on toy mdp
    mdp.setFetchable(dql)

    //start the training
    dql.train()

    //useless on toy but good practice!
    mdp.close()

}

fun hardToy() {

    //record the training data in rl4j-data in a new folder
    val manager = DataManager()

    //define the mdp from toy (toy length)
    val mdp = HardDeteministicToy()

    //define the training
    val dql = QLearningDiscreteDense(mdp, TOY_NET, TOY_QL, manager)

    //start the training
    dql.train()

    //useless on toy but good practice!
    mdp.close()


}


fun toyAsyncNstep() {

    //record the training data in rl4j-data in a new folder
    val manager = DataManager()

    //define the mdp
    val mdp = SimpleToy(20)

    //define the training
    val dql = AsyncNStepQLearningDiscreteDense(mdp, TOY_NET, TOY_ASYNC_QL, manager)

    //enable some logging for debug purposes on toy mdp
    mdp.setFetchable(dql)

    //start the training
    dql.train()

    //useless on toy but good practice!
    mdp.close()

}