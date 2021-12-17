package klite.json

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import klite.*
import klite.StatusCode.Companion.BadRequest
import java.io.InputStream
import java.io.OutputStream
import kotlin.reflect.KClass

fun buildMapper() = jacksonMapperBuilder()
  .addModule(JavaTimeModule())
  .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
  .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
  .serializationInclusion(JsonInclude.Include.NON_NULL)
  .build()

class JsonBody(
  val json: ObjectMapper = buildMapper(),
  override val contentType: String = "application/json"
): BodyParser, BodyRenderer, Extension {
  override fun <T: Any> parse(input: InputStream, type: KClass<T>): T = json.readValue(input, type.java)
  override fun render(output: OutputStream, value: Any?) = json.writeValue(output, value)

  override fun install(server: Server) {
    server.registry.register(json)
    server.errorHandler.apply {
      on(MissingKotlinParameterException::class, BadRequest)
      on(ValueInstantiationException::class, BadRequest)
    }
  }
}

fun Router.enableJson() {
  val json = registry.require<JsonBody>()
  renderer(json)
  parser(json)
}
