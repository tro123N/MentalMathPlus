package com.helddertierwelt.mentalmath.presentation.viewmodel.game

import com.helddertierwelt.mentalmath.R
import kotlinx.serialization.Serializable

@Serializable
enum class Operation(val stringId: Int) : java.io.Serializable {
    ADDITION(R.string.add),
    SUBTRACTION(R.string.subtract),
    MULTIPLICATION(R.string.multiply),
    DIVISION(R.string.divide)
}