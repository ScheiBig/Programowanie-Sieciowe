package com.marcinjeznach.javafx

import javafx.scene.layout.Background
import javafx.scene.paint.Paint

val Paint.asFill: Background get() = Background.fill(this)
