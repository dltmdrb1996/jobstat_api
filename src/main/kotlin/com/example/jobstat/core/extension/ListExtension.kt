package com.example.jobstat.core.extension

// extension null List convert to emptyList
fun <T> List<T>?.orEmptyList(): List<T> = this ?: emptyList()

fun <T> List<T>.toArrayList(): ArrayList<T> = ArrayList(this)

// useful list extension
fun <T> List<T>.toMap(keySelector: (T) -> String): Map<String, T> {
    return this.associateBy(keySelector)
}

// useful list extension
fun <T> List<T>.toMap(keySelector: (T) -> String, valueSelector: (T) -> Any): Map<String, Any> {
    return this.associateBy(keySelector, valueSelector)
}

// useful list extension
fun <T> List<T>.toMap(keySelector: (T) -> String, valueSelector: (T) -> Any, valueFilter: (T) -> Boolean): Map<String, Any> {
    return this.filter { valueFilter(it) }.associateBy(keySelector, valueSelector)
}

// useful list extension
fun <T> List<T>.toMap(keySelector: (T) -> String, valueSelector: (T) -> Any, valueFilter: (T) -> Boolean, valueFilter2: (T) -> Boolean): Map<String, Any> {
    return this.filter { valueFilter(it) && valueFilter2(it) }.associateBy(keySelector, valueSelector)
}



