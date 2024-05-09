package domain

internal sealed interface SapperIntent {
    data class OpenCell(val x: Int, val y: Int) : SapperIntent
    data class ToggleMarkCell(val x: Int, val y: Int) : SapperIntent
    object RestartGame : SapperIntent
    object UpdateSecondTimer : SapperIntent
}
