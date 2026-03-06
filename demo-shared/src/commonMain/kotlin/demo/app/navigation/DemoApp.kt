package demo.app.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DemoApp() {
    var currentScreen by remember { mutableStateOf<DemoScreen?>(null) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val screen = currentScreen
            if (screen == null) {
                DemoHomeScreen(onScreenSelected = { currentScreen = it })
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { currentScreen = null }) {
                            Text("\u2190 Back")
                        }
                        Text(
                            text = screen.title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    HorizontalDivider()
                    screen.content()
                }
            }
        }
    }
}

@Composable
private fun DemoHomeScreen(onScreenSelected: (DemoScreen) -> Unit) {
    LazyColumn {
        item {
            Text(
                text = "Dejavu Demos",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp),
            )
        }
        items(DemoScreen.entries.toList()) { screen ->
            Text(
                text = screen.title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onScreenSelected(screen) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            )
            HorizontalDivider()
        }
    }
}
