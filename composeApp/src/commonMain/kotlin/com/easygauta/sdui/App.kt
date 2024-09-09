package com.easygauta.sdui

import UIBuilder
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {

//        Column {
//            for (i in 1..10) {
//                Spacer(Modifier.height(5.dp))
//                Text("Hello World", style = MaterialTheme.typography.subtitle1)
//            }
//        }

        UIBuilder()

    }
}