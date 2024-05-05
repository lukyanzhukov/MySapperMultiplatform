@file:OptIn(
    ExperimentalResourceApi::class, ExperimentalFoundationApi::class, ExperimentalMaterialApi::class
)

package presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import domain.Cell
import domain.SapperIntent
import domain.SapperState
import domain.SapperStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import ru.lukyanzhukov.mysapper.composeapp.generated.resources.*


private val store = SapperStore(CoroutineScope(SupervisorJob()))

@Composable
fun App() {
    MaterialTheme {
        MainScreen()
    }
}

@Composable
private fun MainScreen() {
    val bottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val bottomSheetContent = remember { mutableStateOf<BottomSheetContent?>(null) }
    val state by store.stateFlow.collectAsState()

    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetContent = { BottomSheetContent(bottomSheetContent.value) }
    ) {
        Scaffold(
            topBar = { MySapperTopBar(bottomSheetState, bottomSheetContent) },
            content = { padding -> ScreenContent(state, padding) },
            bottomBar = {
                Button(
                    content = { Text(stringResource(Res.string.my_sapper_restart_button)) },
                    onClick = remember { { store.send(SapperIntent.RestartGame) } },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth()
                )
            }
        )
    }
}

@Composable
private fun MySapperTopBar(
    bottomSheetState: ModalBottomSheetState,
    bottomSheetContent: MutableState<BottomSheetContent?>,
) {
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text(stringResource(Res.string.my_sapper_topbar_name)) },
        actions = {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Показать меню")
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    onClick = {
                        showMenu = false
                        scope.launch {
                            bottomSheetContent.value = BottomSheetContent.INFO
                            bottomSheetState.show()
                        }
                    }
                ) {
                    Text(stringResource(Res.string.my_sapper_info_menu_item))
                }
                DropdownMenuItem(
                    onClick = {
                        showMenu = false
                        scope.launch {
                            bottomSheetContent.value = BottomSheetContent.ABOUT_DEVELOPER
                            bottomSheetState.show()
                        }
                    }
                ) {
                    Text(stringResource(Res.string.my_sapper_about_developer_item))
                }
            }
        }
    )
}

@Composable
private fun BottomSheetContent(bottomSheetContent: BottomSheetContent?) {
    when (bottomSheetContent) {
        BottomSheetContent.INFO -> {
            BottomSheetContent(
                title = stringResource(Res.string.my_sapper_info_menu_item),
                content = stringResource(Res.string.my_sapper_info)
            )
        }

        BottomSheetContent.ABOUT_DEVELOPER -> {
            BottomSheetContent(
                title = stringResource(Res.string.my_sapper_about_developer_item),
                content = stringResource(Res.string.my_sapper_about_developer)
            )
        }

        else -> Unit
    }
}

@Composable
private fun BottomSheetContent(title: String, content: String) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(text = title, style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = content, style = MaterialTheme.typography.body1)
    }
}

@Composable
private fun ScreenContent(state: SapperState, padding: PaddingValues) {
    Column(
        modifier = Modifier.padding(padding).padding(horizontal = 16.dp)
    ) {
        val text = when (state) {
            is SapperState.Initial -> stringResource(Res.string.my_sapper_start_label)
            is SapperState.Lost -> stringResource(Res.string.my_sapper_lose_label)
            is SapperState.Playing -> stringResource(Res.string.my_sapper_timer_label) + state.gameTimeInSeconds
            is SapperState.Won -> stringResource(Res.string.my_sapper_win_label)
        }
        Text(text = text, modifier = Modifier.padding(bottom = 16.dp))
        if (state !is SapperState.Initial) {
            Text(
                text = stringResource(Res.string.my_sapper_marked_mines_count_label) + "${
                    state.cells.flatten().count { it.isMine && it.isMarked }
                }",
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = stringResource(Res.string.my_sapper_left_mines_count_label) + "${
                    state.cells.flatten().count { it.isMine } - state.cells.flatten()
                        .count { it.isMine && it.isMarked }
                }",
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        GameBoard(
            state = state,
            onCellClicked = remember { { store.send(SapperIntent.OpenCell(it.x, it.y)) } },
            onCellLongPressed = remember { { store.send(SapperIntent.ToggleMarkCell(it.x, it.y)) } },
        )
    }
}

@Composable
private fun GameBoard(
    state: SapperState,
    onCellClicked: (Cell) -> Unit,
    onCellLongPressed: (Cell) -> Unit
) {
    LazyVerticalGrid(columns = GridCells.Fixed(state.cells.first().size)) {
        items(state.cells.flatten()) { cell ->
            CellView(
                cell = cell,
                onCellClicked = remember { { onCellClicked(cell) } },
                onCellLongPressed = remember { { onCellLongPressed(cell) } },
            )
        }
    }
}

@Composable
private fun CellView(cell: Cell, onCellClicked: () -> Unit, onCellLongPressed: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .padding(4.dp)
            .background(
                color = if (cell.isMarked) Color.Yellow else if (cell.isOpen) Color.LightGray else Color.DarkGray,
                shape = RoundedCornerShape(4.dp)
            )
            .combinedClickable(onClick = onCellClicked, onLongClick = onCellLongPressed)
    ) {
        Text(
            text = when {
                cell.isMarked -> stringResource(Res.string.my_sapper_marker)
                cell.isOpen && cell.isMine -> stringResource(Res.string.my_sapper_mine)
                cell.isOpen -> cell.minesAround.toString()
                else -> ""
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (cell.isMine && cell.isOpen) Color.Red else Color.Black,
        )
    }
}
