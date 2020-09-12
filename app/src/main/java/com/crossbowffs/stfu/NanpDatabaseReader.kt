package com.crossbowffs.stfu

class NanpDatabaseReader(private val reader: CsvReader) {
    companion object {
        private const val NANP_DATABASE_SRC = "nationalnanpa.com"
    }

    private val timestamp: String
    private val columns: Map<String, Int>

    init {
        val tsLine = this.reader.nextLine()
        if (tsLine == null || tsLine[0] != "File Date") {
            throw IllegalStateException("Missing file date header")
        }
        this.timestamp = tsLine[1]

        val header = this.reader.nextLine() ?: throw IllegalStateException("Missing column header")
        val columns = mutableMapOf<String, Int>()
        for (i in header.indices) {
            columns[header[i]] = i
        }
        this.columns = columns
    }

    private fun nextEntry(): NanpEntry? {
        val line = this.reader.nextLine() ?: return null
        val ret = mutableMapOf<String, String>()
        for ((col, index) in this.columns) {
            if (index < line.size) {
                val value = line[index]
                if (value.isNotEmpty()) {
                    ret[col] = value
                }
            }
        }

        val areaCode = ret["NPA_ID"]!!
        val assigned = ret["ASSIGNED"] == "Yes"
        val country = ret["COUNTRY"]
        val location = ret["LOCATION"]

        return NanpEntry(
            areaCode,
            assigned,
            country,
            location
        )
    }

    fun readAll(): NanpDatabase {
        val entries = mutableListOf<NanpEntry>()
        while (true) {
            val entry = nextEntry() ?: break
            entries.add(entry)
        }

        return NanpDatabase(
            NANP_DATABASE_SRC,
            this.timestamp,
            entries
        )
    }
}
