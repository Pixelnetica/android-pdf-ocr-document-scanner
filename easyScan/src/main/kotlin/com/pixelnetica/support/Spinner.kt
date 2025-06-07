package com.pixelnetica.support

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Spinner(
    modifier: Modifier = Modifier,
    label: String,
    options: Array<String>,
    index: Int,
    onIndexChanged: (Int) -> Unit,
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = it
        },
        modifier = modifier,
    ) {
        // Disable keyboard
        CompositionLocalProvider(LocalTextInputService provides null) {
            TextField(
                readOnly = true,
                value = options[index],
                onValueChange = { },
                modifier = Modifier.menuAnchor(),
                label = {
                    Text(label)
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
            )
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize(true)
        ) {
            options.forEachIndexed { index, text ->
                DropdownMenuItem(
                    text = {
                        Text(text = text)
                    },
                    onClick = {
                        expanded = false
                        onIndexChanged(index)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun SpinnerPreview() {
    MaterialTheme {
        Surface {
            val label = "Spinner"
            val options = arrayOf("One", "Two", "Three", "Four")
            var index by remember {
                mutableStateOf(0)
            }

            Spinner(
                modifier = Modifier.fillMaxSize(),
                label = label,
                options = options,
                index = index,
                onIndexChanged = {
                    index = it
                })
        }
    }
}

