package data

import com.github.kittinunf.fuel.Fuel
import com.squareup.moshi.*
import java.math.BigDecimal

class BigDecimalAdapter : JsonAdapter<BigDecimal>() {
    override fun fromJson(reader: JsonReader): BigDecimal? {
        return BigDecimal(reader.nextString())
    }

    override fun toJson(writer: JsonWriter, value: BigDecimal?) {
        if (value == null) {
            writer.value("")
        } else {
            writer.value(value.toPlainString())
        }
    }
}

inline fun <reified T> requestList(request: String): List<T> {
    val response = Fuel.get(request).responseString()
    val json = response.third.component1()!!

    val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .add(BigDecimal::class.java, BigDecimalAdapter())
            .build()
    val type = Types.newParameterizedType(List::class.java, T::class.java)
    val adapter = moshi.adapter<List<T>>(type)
    return adapter.fromJson(json)!!
}

infix fun BigDecimal.divideMoney(other: BigDecimal) = divide(other, 20, BigDecimal.ROUND_HALF_UP)