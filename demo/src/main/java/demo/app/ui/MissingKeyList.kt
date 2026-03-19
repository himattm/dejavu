package demo.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import demo.app.GroundTruthCounters

data class ItemData(val id: Int, val label: String)

@Composable
fun MissingKeyListScreen() {
    SideEffect { GroundTruthCounters.increment("missing_key_root") }

    var showKeyed by remember { mutableStateOf(false) }
    var updateCounter by remember { mutableIntStateOf(0) }
    val data = remember {
        (0 until 50).map { ItemData(id = it, label = "Item $it") }.toMutableStateList()
    }

    Column(modifier = Modifier.testTag("missing_key_root").fillMaxSize()) {
        UpdateItemButton(onClick = {
            updateCounter++
            data[25] = data[25].copy(label = "Item 25 (#$updateCounter)")
        })
        MissingKeyToggle(
            showKeyed = showKeyed,
            onToggle = { showKeyed = !showKeyed }
        )
        if (showKeyed) {
            LazyColumn(
                modifier = Modifier
                    .testTag("keyed_list")
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(data, key = { it.id }) { item ->
                    KeyedItemRow(item = item)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .testTag("nokey_list")
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(data) { item ->
                    NoKeyItemRow(item = item)
                }
            }
        }
    }
}

@Composable
fun MissingKeyToggle(showKeyed: Boolean, onToggle: () -> Unit) {
    SideEffect { GroundTruthCounters.increment("missing_key_toggle") }
    Button(
        onClick = onToggle,
        modifier = Modifier
            .testTag("missing_key_toggle")
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(if (showKeyed) "With Key" else "Without Key")
    }
}

@Composable
fun UpdateItemButton(onClick: () -> Unit) {
    SideEffect { GroundTruthCounters.increment("update_item_btn") }
    Button(
        onClick = onClick,
        modifier = Modifier
            .testTag("update_item_btn")
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text("Update Item #25")
    }
}

@Composable
fun NoKeyItemRow(item: ItemData) {
    val tag = "nokey_item_${item.id}"
    SideEffect { GroundTruthCounters.increment(tag) }
    Text(
        text = item.label,
        modifier = Modifier
            .testTag(tag)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun KeyedItemRow(item: ItemData) {
    val tag = "keyed_item_${item.id}"
    SideEffect { GroundTruthCounters.increment(tag) }
    Text(
        text = item.label,
        modifier = Modifier
            .testTag(tag)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
