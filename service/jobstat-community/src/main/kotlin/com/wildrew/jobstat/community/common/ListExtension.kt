package com.wildrew.jobstat.community.common

fun <T> List<T>?.orEmptyList(): List<T> = this ?: emptyList()

fun <T> List<T>.toArrayList(): ArrayList<T> = ArrayList(this)

fun <T> List<T>.toMap(keySelector: (T) -> String): Map<String, T> = this.associateBy(keySelector)

fun <T> List<T>.toMap(
    keySelector: (T) -> String,
    valueSelector: (T) -> Any,
): Map<String, Any> = this.associateBy(keySelector, valueSelector)

fun <T> List<T>.toMap(
    keySelector: (T) -> String,
    valueSelector: (T) -> Any,
    valueFilter: (T) -> Boolean,
): Map<String, Any> = this.filter { valueFilter(it) }.associateBy(keySelector, valueSelector)

fun <T> List<T>.toMap(
    keySelector: (T) -> String,
    valueSelector: (T) -> Any,
    valueFilter: (T) -> Boolean,
    valueFilter2: (T) -> Boolean,
): Map<String, Any> = this.filter { valueFilter(it) && valueFilter2(it) }.associateBy(keySelector, valueSelector)
