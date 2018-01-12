package util.lang

fun <K, V1, V2, R> Map<K, V1>.zipValues(other: Map<K, V2>, zipper: (V1, V2) -> R): Map<K, R> {
    val result = HashMap<K, R>()
    for (entry in this) {
        val key = entry.key
        result[key] = zipper(entry.value, other[key]!!)
    }
    return result
}