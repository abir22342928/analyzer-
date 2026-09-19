package com.example.model

enum class SignalType(val title: String, val badge: String) {
    POSSIBLE_UP("POSSIBLE UP", "🟢"),
    POSSIBLE_DOWN("POSSIBLE DOWN", "🔴"),
    WAIT("WAIT", "🟡")
}
