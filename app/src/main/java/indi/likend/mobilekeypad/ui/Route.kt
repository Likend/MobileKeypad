package indi.likend.mobilekeypad.ui

import kotlinx.serialization.Serializable

data object Route {
    @Serializable
    data object Home

    @Serializable
    data object Settings

    @Serializable
    data object Connection
}
