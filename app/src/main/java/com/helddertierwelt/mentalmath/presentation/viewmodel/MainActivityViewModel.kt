package com.helddertierwelt.mentalmath.presentation.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainActivityViewModel : ViewModel() {
    private val _showAlreadyPlayedPopUp = MutableStateFlow(false)
    val showAlreadyPlayedPopUp: StateFlow<Boolean> = _showAlreadyPlayedPopUp

    private val _showDeepLinkPopUp = MutableStateFlow(false)
    val showDeepLinkPopUp: StateFlow<Boolean> = _showDeepLinkPopUp

    fun setShowAlreadyPlayedPopUp(value: Boolean) {
        _showAlreadyPlayedPopUp.value = value
    }

    fun setShowDeepLinkPopUp(value: Boolean) {
        _showDeepLinkPopUp.value = value
    }
}

