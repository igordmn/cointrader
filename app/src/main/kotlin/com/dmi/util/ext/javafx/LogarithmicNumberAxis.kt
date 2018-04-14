package com.dmi.util.ext.javafx

import com.sun.javafx.charts.ChartLayoutAnimator
import java.text.NumberFormat
import java.util.ArrayList
import java.util.LinkedList
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.beans.binding.DoubleBinding
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.chart.ValueAxis
import javafx.util.Duration

//https://stackoverflow.com/a/22424519
//http://blog.dooapp.com/logarithmic-scale-strikes-back-in-javafx-20
//Edited by Vadim Levit & Benny Lutati for usage in AgentZero ( https://code.google.com/p/azapi-test/ )
class LogarithmicNumberAxis : ValueAxis<Number> {

    private var currentAnimationID: Any? = null
    private val animator = ChartLayoutAnimator(this)

    //Create our LogarithmicAxis class that extends ValueAxis<Number> and define two properties that will represent the log lower and upper bounds of our axis.
    private val logUpperBound = SimpleDoubleProperty()
    private val logLowerBound = SimpleDoubleProperty()
    //

    //we bind our properties with the default bounds of the value axis. But before, we should verify the given range according to the mathematic logarithmic interval definition.
    constructor() : super(1.0, 10000000.0) {
        bindLogBoundsToDefaultBounds()
    }

    constructor(lowerBound: Double, upperBound: Double) : super(lowerBound, upperBound) {
        validateBounds(lowerBound, upperBound)
        bindLogBoundsToDefaultBounds()
    }

    fun setLogarithmizedUpperBound(d: Double) {
        val nd = Math.pow(10.0, Math.ceil(Math.log10(d)))
        upperBound = if (nd == d) nd * 10 else nd
    }

    /**
     * Bind our logarithmic bounds with the super class bounds, consider the
     * base 10 logarithmic scale.
     */
    private fun bindLogBoundsToDefaultBounds() {
        logLowerBound.bind(object : DoubleBinding() {
            init {
                super.bind(lowerBoundProperty())
            }

            override fun computeValue(): Double {
                return Math.log10(lowerBoundProperty().get())
            }
        })
        logUpperBound.bind(object : DoubleBinding() {
            init {
                super.bind(upperBoundProperty())
            }

            override fun computeValue(): Double {
                return Math.log10(upperBoundProperty().get())
            }
        })
    }

    /**
     * Validate the bounds by throwing an exception if the values are not
     * conform to the mathematics log interval: ]0,Double.MAX_VALUE]
     *
     * @param lowerBound
     * @param upperBound
     * @throws IllegalLogarithmicRangeException
     */
    @Throws(IllegalLogarithmicRangeException::class)
    private fun validateBounds(lowerBound: Double, upperBound: Double) {
        if (lowerBound < 0 || upperBound < 0 || lowerBound > upperBound) {
            throw IllegalLogarithmicRangeException(
                    "The logarithmic range should be in [0,Double.MAX_VALUE] and the lowerBound should be less than the upperBound")
        }
    }

    //Now we have to implement all abstract methods of the ValueAxis class.
    //The first one, calculateMinorTickMarks is used to get the list of minor tick marks position that you want to display on the axis. You could find my definition below. It's based on the number of minor tick and the logarithmic formula.
    override fun calculateMinorTickMarks(): List<Number> {
        return ArrayList()
    }

    //Then, the calculateTickValues method is used to calculate a list of all the data values for each tick mark in range, represented by the second parameter. The formula is the same than previously but here we want to display one tick each power of 10.
    override fun calculateTickValues(length: Double, range: Any?): List<Number> {
        val tickPositions = LinkedList<Number>()
        if (range != null) {
            val lowerBound = (range as DoubleArray)[0]
            val upperBound = range[1]

            var i = Math.log10(lowerBound)
            while (i <= Math.log10(upperBound)) {
                tickPositions.add(Math.pow(10.0, i))
                i++
            }

            if (!tickPositions.isEmpty()) {
                if (tickPositions.last.toDouble() != upperBound) {
                    tickPositions.add(upperBound)
                }
            }
        }

        return tickPositions
    }

    /**
     * The getRange provides the current range of the axis. A basic
     * implementation is to return an array of the lowerBound and upperBound
     * properties defined into the ValueAxis class.
     *
     * @return
     */
    override fun getRange(): DoubleArray {
        return doubleArrayOf(lowerBound, upperBound)
    }

    /**
     * The getTickMarkLabel is only used to convert the number value to a string
     * that will be displayed under the tickMark. Here I choose to use a number
     * formatter.
     *
     * @param value
     * @return
     */
    override fun getTickMarkLabel(value: Number): String {
        val formatter = NumberFormat.getInstance()
        formatter.maximumIntegerDigits = 10
        formatter.minimumIntegerDigits = 1
        return formatter.format(value)
    }

    /**
     * The method setRange is used to update the range when data are added into
     * the chart. There is two possibilities, the axis is animated or not. The
     * simplest case is to set the lower and upper bound properties directly
     * with the new values.
     *
     * @param range
     * @param animate
     */
    override fun setRange(range: Any?, animate: Boolean) {
        if (range != null) {
            val rangeProps = range as DoubleArray?
            val lowerBound = rangeProps!![0]
            val upperBound = rangeProps[1]

            val oldLowerBound = getLowerBound()
            setLowerBound(lowerBound)
            setUpperBound(upperBound)
            if (animate) {
                animator.stop(currentAnimationID)
                currentAnimationID = animator.animate(
                        KeyFrame(Duration.ZERO,
                                KeyValue(currentLowerBound, oldLowerBound)
                        ),
                        KeyFrame(Duration.millis(700.0),
                                KeyValue(currentLowerBound, lowerBound)
                        )
                )
            } else {
                currentLowerBound.set(lowerBound)
            }
        }
    }

    /**
     * We are almost done but we forgot to override 2 important methods that are
     * used to perform the matching between data and the axis (and the reverse).
     *
     * @param displayPosition
     * @return
     */
    override fun getValueForDisplay(displayPosition: Double): Number {
        val delta = logUpperBound.get() - logLowerBound.get()
        return if (side.isVertical) {
            Math.pow(10.0, (displayPosition - height) / -height * delta + logLowerBound.get())
        } else {
            Math.pow(10.0, displayPosition / width * delta + logLowerBound.get())
        }
    }

    override fun getDisplayPosition(value: Number): Double {
        val delta = logUpperBound.get() - logLowerBound.get()
        val deltaV = Math.log10(value.toDouble()) - logLowerBound.get()
        return if (side.isVertical) {
            (1.0 - deltaV / delta) * height
        } else {
            deltaV / delta * width
        }
    }

    /**
     * Exception to be thrown when a bound value isn't supported by the
     * logarithmic axis<br></br>
     *
     *
     * @author Kevin Senechal mailto: kevin.senechal@dooapp.com
     */
    inner class IllegalLogarithmicRangeException(message: String) : RuntimeException(message)
}