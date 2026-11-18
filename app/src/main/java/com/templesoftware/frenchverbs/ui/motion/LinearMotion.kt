package com.templesoftware.frenchverbs.ui.motion

/** Project from a monotonic clock; sampling frequency never changes the trajectory. */
fun linearPosition(origin: Double, velocityPerSecond: Double, elapsedMillis: Double): Double =
    origin + velocityPerSecond * elapsedMillis.coerceAtLeast(0.0) / 1000.0
