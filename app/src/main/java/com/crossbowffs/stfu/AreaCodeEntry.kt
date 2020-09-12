package com.crossbowffs.stfu

data class AreaCodeEntry(val name: String, val areaCodes: List<String>) {
    companion object {
        fun from(db: NanpDatabase): List<AreaCodeEntry> {
            return db.entries
                .filter { it.country == "US" && it.location != null }
                .groupBy { it.location!! }
                .map { (area, entry) ->
                    AreaCodeEntry(area, entry.map { it.areaCode })
                }
                .sortedBy { it.name }
        }
    }
}
