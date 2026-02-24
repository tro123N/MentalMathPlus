package com.helddertierwelt.mentalmath101.presentation.viewmodel.game

import kotlinx.serialization.Serializable

@Serializable
data class TaskResult(
    val operand1: Int,
    val operand2: Int,
    val operator: Operation,
    val correctResult: Int,
    val userInput: String,
) : java.io.Serializable