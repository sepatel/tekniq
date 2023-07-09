package io.tekniq

import java.time.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <S, T> Iterable<S>.iteratorAs(action: (src: S) -> T): Iterator<T> {
    val src = iterator()
    return object : Iterator<T> {
        override fun hasNext(): Boolean = src.hasNext()
        override fun next(): T =
            if (!hasNext()) throw NoSuchElementException()
            else action(src.next())
    }
}

fun Date.add(duration: Duration): Date = add(duration.toMillis().toInt(), TimeUnit.MILLISECONDS)
fun Date.add(amount: Int, unit: TimeUnit = TimeUnit.DAYS): Date = Calendar.getInstance()
    .also { it.timeInMillis = this.time }
    .also {
        when (unit) {
            TimeUnit.NANOSECONDS -> throw UnsupportedOperationException("Nanoseconds are not supported by Date")
            TimeUnit.MICROSECONDS -> throw UnsupportedOperationException("Nanoseconds are not supported by Date")
            TimeUnit.MILLISECONDS -> it.add(Calendar.MILLISECOND, amount)
            TimeUnit.SECONDS -> it.add(Calendar.SECOND, amount)
            TimeUnit.MINUTES -> it.add(Calendar.MINUTE, amount)
            TimeUnit.HOURS -> it.add(Calendar.HOUR_OF_DAY, amount)
            TimeUnit.DAYS -> it.add(Calendar.DAY_OF_MONTH, amount)
        }
    }
    .time

fun Date.noTime(zoneId: ZoneId = ZoneId.of("UTC")) =
    toInstant().atZone(zoneId).toLocalDate().atStartOfDay()

fun LocalDate.toDate() = Date.from(atStartOfDay(ZoneOffset.UTC).toInstant())
fun LocalDateTime.toDate() = Date.from(toInstant(ZoneOffset.UTC))

inline fun <reified V> init(
    noinline default: (property: KProperty<*>) -> V = { throw IllegalStateException("${it.name} is not initialized") }
): ReadWriteProperty<Any, V> = InitProperty(default)

class InitProperty<V>(private val default: (property: KProperty<*>) -> V) : ReadWriteProperty<Any, V> {
    private object EMPTY

    private var value: Any? = EMPTY

    override fun getValue(thisRef: Any, property: KProperty<*>): V =
        if (value == EMPTY) default(property)
        else value as V

    override fun setValue(thisRef: Any, property: KProperty<*>, value: V) {
        if (this.value != EMPTY) error("${property.name} already initialized")
        this.value = value
    }
}

inline fun <reified V> lazyChild(parent: String, noinline initializer: () -> V): ReadOnlyProperty<Any, V> =
    LazyChildProperty(parent, initializer)

class LazyChildProperty<V>(private val parent: String, private val initializer: () -> V) : ReadOnlyProperty<Any, V> {
    private val value: V by lazy { initializer() }

    override fun getValue(thisRef: Any, property: KProperty<*>): V {
        // 1st stack item is calling this lazy child function
        // 2nd stack item is the variable being protected
        // 3rd stack item is the caller to be audited
        val throwable = Throwable("Property cannot be accessed outside of $parent")
        if (throwable.stackTrace[2].className.startsWith(parent)) return value
        throwable.printStackTrace()
        throw throwable
    }
}
