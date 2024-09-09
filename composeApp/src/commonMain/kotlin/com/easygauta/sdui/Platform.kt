package com.easygauta.sdui

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform