package com.example.jobstat.core.base.mongo.converter

import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverter
import io.swagger.v3.core.converter.ModelConverterContext
import io.swagger.v3.oas.models.media.Schema
import org.slf4j.LoggerFactory
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class CustomModelConverter : ModelConverter {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun resolve(
        annotatedType: AnnotatedType,
        context: ModelConverterContext,
        chain: Iterator<ModelConverter>,
    ): Schema<*>? {
        // 1) 체인에서 다른 컨버터 우선 적용
        val resolvedSchema =
            if (chain.hasNext()) {
                chain.next().resolve(annotatedType, context, chain)
            } else {
                null
            }

        // 2) 타입이 "ApiResponse<T>"인지 판별
        val type = annotatedType.type
        if (type !is ParameterizedType) return resolvedSchema

        val rawClass = type.rawType as? Class<*> ?: return resolvedSchema
        if (rawClass.canonicalName != "com.example.jobstat.core.global.wrapper.ApiResponse") {
            return resolvedSchema
        }

        // T 추출
        val actualArgs = type.actualTypeArguments
        if (actualArgs.isEmpty()) return resolvedSchema
        val innerType = actualArgs[0]

        // 3) 스키마명 만들기: "ApiResponseOfXxx"
        val modelName = buildModelName(rawClass.simpleName, innerType)
        if (context.definedModels[modelName] != null) {
            // 이미 등록된 스키마 있으면 재활용
            return context.definedModels[modelName]
        }

        log.debug("Generating custom schema for $modelName")

        // 4) ApiResponse 필드 -> code, status, message, data
        val wrapperSchema = Schema<Any>()
        wrapperSchema.name = modelName
        wrapperSchema.type = "object"

        // code
        val codeSchema = Schema<Any>()
        codeSchema.type = "integer"
        codeSchema.description = "응답 코드"
        wrapperSchema.properties["code"] = codeSchema

        // status
        val statusSchema = Schema<Any>()
        statusSchema.type = "string"
        statusSchema.description = "HTTP 상태"
        wrapperSchema.properties["status"] = statusSchema

        // message
        val msgSchema = Schema<Any>()
        msgSchema.type = "string"
        msgSchema.description = "응답 메시지"
        wrapperSchema.properties["message"] = msgSchema

        // data -> T
        val dataSchema = resolveTypeRef(context, innerType)
        wrapperSchema.properties["data"] = dataSchema

        // 5) 스키마 등록
        context.defineModel(modelName, wrapperSchema)
        return wrapperSchema
    }

    private fun buildModelName(
        wrapperName: String,
        innerType: Type,
    ): String = wrapperName + "Of" + extractClassName(innerType)

    private fun extractClassName(t: Type): String =
        when (t) {
            is Class<*> -> t.simpleName
            is ParameterizedType -> {
                val raw = t.rawType as? Class<*>
                val rawName = raw?.simpleName ?: "UnknownType"
                val subArgs = t.actualTypeArguments
                val subNames = subArgs.joinToString("And") { extractClassName(it) }
                "${rawName}Of$subNames"
            }
            else -> "UnknownType"
        }

    private fun resolveTypeRef(
        context: ModelConverterContext,
        type: Type,
    ): Schema<*> {
        val annotated = AnnotatedType(type).resolveAsRef(true)
        val resolved = context.resolve(annotated)
        return resolved ?: Schema<Any>().apply {
            this.type = "object"
            this.description = "Unknown structure"
        }
    }
}
