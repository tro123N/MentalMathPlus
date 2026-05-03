/*
Mental Math - Android app for practicing mental arithmetic
Copyright (C) 2025-2026 HeldDerTierwelt

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see https://www.gnu.org/licenses/gpl-3.0.md.
*/

package com.helddertierwelt.mentalmath.presentation.viewmodel.mainactivity

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

    fun resetMainActivity() {
        setShowAlreadyPlayedPopUp(false)
        setShowDeepLinkPopUp(false)
    }
}