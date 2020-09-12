package com.crossbowffs.stfu

data class NanpDatabase(
    val source: String,
    val timestamp: String,
    val entries: List<NanpEntry>
)
