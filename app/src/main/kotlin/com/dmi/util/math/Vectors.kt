package com.dmi.util.math

operator fun List<Double>.times(other: List<Double>) = zip(other, Double::times)