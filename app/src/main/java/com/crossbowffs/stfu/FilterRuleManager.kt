package com.crossbowffs.stfu

import android.content.Context
import android.content.SharedPreferences

class FilterRuleManager(context: Context) {
    companion object {
        private const val PREF_FILE = "filter_rules"
        private const val PREF_KEY = "rules"

        fun isValidRule(rule: CharSequence): Boolean {
            return rule.matches(Regex("^\\+\\d*$"))
        }

        private fun deserializeRules(serialized: String): List<String> {
            return if (serialized.isEmpty()) {
                emptyList()
            } else {
                serialized.split(";")
            }
        }

        private fun serializeRules(deserialized: List<String>): String {
            return deserialized.joinToString(";")
        }
    }

    private val prefs: SharedPreferences
    private val rules: MutableList<String>

    init {
        prefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        rules = mutableListOf()
        readRules()
    }

    private fun readRules() {
        rules.clear()
        rules.addAll(deserializeRules(prefs.getString(PREF_KEY, null) ?: ""))
    }

    private fun writeRules() {
        prefs.edit().putString(PREF_KEY, serializeRules(rules)).apply()
    }

    fun getByIndex(i: Int): String {
        return rules[i]
    }

    fun getCount(): Int {
        return rules.size
    }

    fun addRules(newRules: List<String>): Int {
        val existingRules = rules.toHashSet()
        var count = 0
        for (newRule in newRules) {
            if (existingRules.contains(newRule)) {
                continue
            }
            if (!isValidRule(newRule)) {
                throw IllegalArgumentException("Invalid rule: $newRule")
            }
            rules.add(newRule)
            count++
        }
        writeRules()
        return count
    }

    fun deleteByIndex(i: Int) {
        if (i < 0 || i >= rules.size) {
            throw IllegalArgumentException("Index out of bounds")
        }
        rules.removeAt(i)
        writeRules()
    }

    fun isBlocked(value: String): Boolean {
        return rules.any { value.startsWith(it) }
    }
}
