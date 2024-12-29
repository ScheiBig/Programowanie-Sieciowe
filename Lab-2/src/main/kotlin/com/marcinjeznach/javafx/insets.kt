package com.marcinjeznach.javafx

import javafx.geometry.Insets as FxInsets


fun Insets(
	top: Double = 0.0,
	right: Double = 0.0,
	bottom: Double = 0.0,
	left: Double = 0.0,
) = FxInsets(top, right, bottom, left)

fun Insets(
	top: Double,
	rightLeft: Double,
	bottom: Double,
) = Insets(top, rightLeft, bottom, rightLeft)

fun Insets(
	topBottom: Double,
	rightLeft: Double,
) = Insets(topBottom, rightLeft, topBottom, rightLeft)

fun Insets(
	topRightBottomLeft: Double,
) = Insets(topRightBottomLeft, topRightBottomLeft, topRightBottomLeft, topRightBottomLeft)
