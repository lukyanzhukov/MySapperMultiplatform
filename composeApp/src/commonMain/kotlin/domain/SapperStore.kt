package domain

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlin.math.abs
import kotlin.random.Random

internal class SapperStore(private val coroutineScope: CoroutineScope) {

    private companion object {
        const val TIMER_TICK_DELAY_MSEC = 1000L
        const val INITIAL_WIDTH = 10
        const val INITIAL_HEIGHT = 10
        const val INITIAL_MINES_COUNT = 15
    }

    val stateFlow: StateFlow<SapperState> get() = mutableStateFlow.asStateFlow()

    private val mutableStateFlow: MutableStateFlow<SapperState> =
        MutableStateFlow(
            SapperState.Initial.getInitialState(
                width = INITIAL_WIDTH,
                height = INITIAL_HEIGHT,
                minesCount = INITIAL_MINES_COUNT,
            )
        )

    private val channel: Channel<SapperIntent> = Channel(Channel.UNLIMITED)

    init {
        coroutineScope.launch {
            channel.consumeAsFlow().collect { action ->
                mutableStateFlow.value = reduceAction(mutableStateFlow.value, action)
            }
        }

        CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                if (mutableStateFlow.value is SapperState.Playing) {
                    delay(TIMER_TICK_DELAY_MSEC)
                    send(SapperIntent.UpdateSecondTimer)
                }
            }
        }
    }

    fun send(action: SapperIntent) {
        coroutineScope.launch {
            channel.send(action)
        }
    }

    private fun reduceAction(state: SapperState, action: SapperIntent): SapperState {
        return when (action) {
            is SapperIntent.OpenCell -> processOpenCell(state, action)
            is SapperIntent.ToggleMarkCell -> processToggleMarkCell(state, action)
            is SapperIntent.RestartGame -> processRestartGame(state)
            is SapperIntent.UpdateSecondTimer -> processUpdateSecondTimer(state)
        }
    }

    private fun processOpenCell(currentState: SapperState, action: SapperIntent.OpenCell): SapperState {
        return when (currentState) {
            is SapperState.Initial -> processInitialOpenCell(
                cells = currentState.cells,
                mines = currentState.minesCount,
                x = action.x,
                y = action.y,
                width = currentState.width,
                height = currentState.height
            )

            is SapperState.Playing -> processPlayingOpenCell(action.x, action.y, currentState)

            else -> currentState
        }
    }

    private fun processInitialOpenCell(
        cells: List<List<Cell>>,
        mines: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ): SapperState {
        val updatedCells = updatedCells(
            x = x,
            y = y,
            width = width,
            height = height,
            cells = placeMines(cells.flatten(), mines, x, y, width, height),
        )

        return SapperState.Playing(
            width = width,
            cells = updatedCells.chunked(width),
            minesCount = mines,
            height = height,
        )
    }

    private fun processPlayingOpenCell(x: Int, y: Int, currentState: SapperState.Playing): SapperState {
        val index = x + y * currentState.width
        val cells = currentState.cells.flatten().toMutableList()
        val cell = cells[index]

        return when {
            !cell.isOpen && !cell.isMarked && cell.isMine -> {
                cells[index] = cell.copy(isOpen = true)
                SapperState.Lost(
                    width = currentState.width,
                    height = currentState.height,
                    minesCount = currentState.minesCount,
                    cells = cells.chunked(currentState.width)
                )
            }

            !cell.isOpen && !cell.isMarked -> {
                val updatedCells = updatedCells(
                    x = x,
                    y = y,
                    width = currentState.width,
                    height = currentState.height,
                    cells = currentState.cells.flatten(),
                )

                if (updatedCells.count { !it.isMine && !it.isOpen } == 0) {
                    SapperState.Won(
                        width = currentState.width,
                        height = currentState.height,
                        minesCount = currentState.minesCount,
                        cells = updatedCells.chunked(currentState.width),
                    )
                } else {
                    currentState.copy(cells = updatedCells.chunked(currentState.width))
                }
            }

            else -> currentState
        }
    }

    private fun updatedCells(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        cells: List<Cell>
    ): List<Cell> {
        val index = x + y * width
        if (cells[index].isOpen || cells[index].isMine) return cells

        val updatedCells = cells.toMutableList()
        updatedCells[index] = updatedCells[index].copy(isOpen = true)

        return if (cells[index].minesAround == 0) {
            getSurroundingCells(
                x,
                y,
                width,
                height,
                updatedCells
            ).fold(updatedCells.toList()) { acc, adjacentCell ->
                updatedCells(adjacentCell.x, adjacentCell.y, width, height, acc)
            }
        } else {
            updatedCells
        }
    }

    private fun placeMines(
        cells: List<Cell>,
        mines: Int,
        firstX: Int,
        firstY: Int,
        width: Int,
        height: Int
    ): List<Cell> {
        val random = Random.Default
        var minesPlaced = 0
        val mutableCells = cells.toMutableList()

        while (minesPlaced < mines) {
            val index = random.nextInt(cells.size)
            val cell = mutableCells[index]

            if ((cell.x != firstX || cell.y != firstY) && !cell.isMine) {
                mutableCells[index] = cell.copy(isMine = true)
                minesPlaced++
            }
        }

        return mutableCells.map { cell ->
            val surroundingCells = getSurroundingCells(cell.x, cell.y, width, height, mutableCells)
            val minesCount = surroundingCells.count { it.isMine }
            cell.copy(minesAround = minesCount)
        }
    }

    private fun getSurroundingCells(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        cells: List<Cell>
    ): List<Cell> {
        return (-1..1).flatMap { dx ->
            (-1..1).mapNotNull { dy ->
                val nx = x + dx
                val ny = y + dy
                if (nx in 0..<width && ny >= 0 && ny < height) {
                    if ((x == nx || y == ny) || (abs(nx - x) == 1 && abs(ny - y) == 1)) {
                        cells[nx + ny * width]
                    } else null
                } else null
            }
        }
    }

    private fun processToggleMarkCell(currentState: SapperState, intent: SapperIntent.ToggleMarkCell): SapperState {
        return if (currentState is SapperState.Playing) {
            val index = intent.x + intent.y * currentState.width
            val cells = currentState.cells.flatten().toMutableList()
            val cell = cells[index]

            if (!cell.isOpen) {
                cells[index] = cell.copy(isMarked = !cell.isMarked)
                currentState.copy(cells = cells.chunked(currentState.width))
            } else {
                currentState
            }
        } else {
            currentState
        }
    }

    private fun processUpdateSecondTimer(currentState: SapperState): SapperState {
        return if (currentState is SapperState.Playing) {
            currentState.copy(gameTimeInSeconds = currentState.gameTimeInSeconds + 1)
        } else {
            currentState
        }
    }

    private fun processRestartGame(currentState: SapperState): SapperState = SapperState.Initial.getInitialState(
        width = currentState.width,
        height = currentState.height,
        minesCount = currentState.minesCount,
    )
}