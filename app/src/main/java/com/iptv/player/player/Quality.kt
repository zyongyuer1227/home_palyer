package com.iptv.player.player

enum class NasQuality(val label: String, val h: Int?) {
    ORIGINAL("原画", null),
    Q1080("1080P", 1080),
    Q720("720P", 720),
    Q480("480P", 480);

    companion object {
        fun fromHeight(h: Int?): NasQuality =
            values().firstOrNull { it.h == h } ?: ORIGINAL

        fun fromLabel(label: String?): NasQuality =
            values().firstOrNull { it.label == label } ?: ORIGINAL
    }
}
