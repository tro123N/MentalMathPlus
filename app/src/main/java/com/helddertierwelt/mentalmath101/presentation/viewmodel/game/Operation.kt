package com.helddertierwelt.mentalmath101.presentation.viewmodel.game

import com.helddertierwelt.mentalmath101.R
import kotlinx.serialization.Serializable

@Serializable
enum class Operation(val stringId: Int) : java.io.Serializable {
    ADDITION(R.string.add),
    SUBTRACTION(R.string.subtract),
    MULTIPLICATION(R.string.multiply),
    DIVISION(R.string.divide)
}