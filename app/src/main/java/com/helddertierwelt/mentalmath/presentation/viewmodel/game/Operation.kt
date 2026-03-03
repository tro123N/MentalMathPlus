package com.helddertierwelt.mentalmath.presentation.viewmodel.game

import kotlinx.serialization.Serializable

@Serializable
enum class Operation(val symbol: String) : java.io.Serializable {
    ADDITION("+"),
    SUBTRACTION("−"),
    MULTIPLICATION("×"),
    DIVISION("÷")
}