package com.easygauta.sdui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import org.w3c.dom.get

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    println(kotlinx.browser.window.location.pathname)
    document.getElementById("root-container")?.let {
        ComposeViewport(it) {
            App()
        }
    } ?: println("No root container found")
}