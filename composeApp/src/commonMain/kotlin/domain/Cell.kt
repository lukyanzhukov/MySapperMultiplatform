package domain

import androidx.compose.runtime.Immutable

@Immutable
internal data class Cell(
    val x: Int,
    val y: Int,
    val isMine: Boolean = false,
    val isOpen: Boolean = false,
    val isMarked: Boolean = false,
    val minesAround: Int = 0
)
