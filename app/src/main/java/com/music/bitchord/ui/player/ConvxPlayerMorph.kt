package com.music.bitchord.ui.player

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

/** Minimal Convx player-morph signals the vendored nav bar reads. */
object PlayerMorph {
    val glassFrozen: Boolean get() = false
    fun landingDipPx(): Float = 0f
    val pillShadow: State<Float> = mutableStateOf(1f)
}
