package klite

import java.lang.reflect.InvocationTargetException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf

typealias ToStringConverter<T> = (s: String) -> T

object Converter {
  private val converters: MutableMap<KClass<*>, ToStringConverter<*>> = ConcurrentHashMap(mapOf(
    UUID::class to UUID::fromString,
    Currency::class to Currency::getInstance
  ))

  operator fun <T: Any> set(type: KClass<T>, converter: ToStringConverter<T>) { converters[type] = converter }
  inline fun <reified T: Any> use(noinline converter: ToStringConverter<T>) = set(T::class, converter)

  fun supports(type: KClass<*>) = converters[type] != null

  inline fun <reified T: Any> fromString(s: String) = fromString(s, T::class)

  @Suppress("UNCHECKED_CAST")
  fun <T: Any> fromString(s: String, type: KType): T = fromString(s, type.classifier as KClass<T>)

  @Suppress("UNCHECKED_CAST")
  fun <T: Any> fromString(s: String, type: KClass<T>): T =
    (converters[type] as? ToStringConverter<T> ?: findCreator(type).also { converters[type] = it }).invoke(s)

  private fun <T: Any> findCreator(type: KClass<T>): ToStringConverter<T> =
    if (type.isSubclassOf(Enum::class)) enumCreator(type) else
    try { constructorCreator(type) }
    catch (e: NoSuchMethodException) {
      try { parseMethodCreator(type) }
      catch (e2: NoSuchMethodException) {
        error("Don't know how to convert String to $type:\n$e\n$e2")
      }
    }

  private fun <T: Any> enumCreator(type: KClass<T>): ToStringConverter<T> {
    val enumConstants = type.java.enumConstants
    return { s -> enumConstants.find { (it as Enum<*>).name == s } ?: error("No $type constant: $s") }
  }

  private fun <T: Any> constructorCreator(type: KClass<T>): ToStringConverter<T> {
    val constructor = type.javaObjectType.getDeclaredConstructor(String::class.java)
    if (type.hasAnnotation<JvmInline>()) constructor.isAccessible = true
    return { s ->
      try { constructor.newInstance(s) }
      catch (e: InvocationTargetException) { throw e.targetException }
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun <T: Any> parseMethodCreator(type: KClass<T>): ToStringConverter<T> {
    val parse = type.java.getMethod("parse", CharSequence::class.java)
    return { s ->
      try { parse.invoke(null, s) as T }
      catch (e: InvocationTargetException) { throw e.targetException }
    }
  }
}
