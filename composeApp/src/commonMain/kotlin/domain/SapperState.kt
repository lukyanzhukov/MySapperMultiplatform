package domain

import androidx.compose.runtime.Immutable

@Immutable
internal sealed class SapperState {
    abstract val width: Int
    abstract val height: Int
    abstract val minesCount: Int
    abstract val cells: List<List<Cell>>

    data class Initial(
        override val width: Int,
        override val height: Int,
        override val minesCount: Int,
        override val cells: List<List<Cell>>,
    ) : SapperState() {

        companion object {
            fun getInitialState(
                width: Int,
                height: Int,
                minesCount: Int,
            ): Initial {
                return Initial(
                    width = width,
                    height = height,
                    minesCount = minesCount,
                    cells = List(height) { y -> List(width) { x -> Cell(x, y) } },
                )
            }
        }
    }

    data class Playing(
        override val width: Int,
        override val height: Int,
        override val minesCount: Int,
        override val cells: List<List<Cell>>,
        val gameTimeInSeconds: Int = 0,
    ) : SapperState()

    data class Won(
        override val width: Int,
        override val height: Int,
        override val minesCount: Int,
        override val cells: List<List<Cell>>,
    ) : SapperState()

    data class Lost(
        override val width: Int,
        override val height: Int,
        override val minesCount: Int,
        override val cells: List<List<Cell>>,
    ) : SapperState()
}

