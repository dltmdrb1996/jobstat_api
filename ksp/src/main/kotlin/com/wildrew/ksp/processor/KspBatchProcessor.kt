package com.wildrew.ksp.processor

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import java.time.LocalDateTime

class KspBatchProcessor : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment
    ): SymbolProcessor = object : SymbolProcessor {
        private val systemColumns = setOf("created_at", "updated_at")

        override fun process(resolver: Resolver): List<KSAnnotated> {
            resolver.getSymbolsWithAnnotation("jakarta.persistence.Entity")
                .filterIsInstance<KSClassDeclaration>()
                .forEach { classDeclaration ->
                    try {
                        generateMapperFile(classDeclaration, environment)
                    } catch (e: Exception) {
                        environment.log.error(
                            "Failed to generate mapper for ${classDeclaration.qualifiedName?.asString()}: ${e.message}",
                            classDeclaration
                        )
                    }
                }
            return emptyList()
        }

        private fun generateMapperFile(
            classDeclaration: KSClassDeclaration,
            environment: SymbolProcessorEnvironment
        ) {
            val packageName = classDeclaration.packageName.asString()
            val className = classDeclaration.simpleName.asString()

            // Extract the ID type from the superclass BaseEntity<ID>
            val baseEntityType = classDeclaration.superTypes
                .map { it.resolve() }
                .find {
                    val declaration = it.declaration
                    val qualifiedName = declaration.qualifiedName?.asString()
                    qualifiedName == "com.wildrew.batch.common.base.BaseEntity"
                }

            val idTypeName = baseEntityType?.arguments?.firstOrNull()?.type?.resolve()
                ?.declaration?.qualifiedName?.asString() ?: "kotlin.Long"

            val properties = collectAllProperties(classDeclaration, idTypeName)

            val fileContent = buildString {
                append("package $packageName\n\n")
                append(generateImports(classDeclaration, properties, idTypeName))
                append("\n\n")
                append(generateMapperClass(className, properties, idTypeName, classDeclaration))
            }

            environment.codeGenerator.createNewFile(
                dependencies = Dependencies(false, classDeclaration.containingFile!!),
                packageName = packageName,
                fileName = "${className}Mapper"
            ).use {
                it.write(fileContent.toByteArray())
            }
        }

        private fun collectAllProperties(
            classDeclaration: KSClassDeclaration,
            idTypeName: String
        ): List<PropertyInfo> {
            val properties = mutableMapOf<String, PropertyInfo>()

            // Add BaseEntity properties explicitly
            properties["id"] = PropertyInfo(
                name = "id",
                columnName = "id",
                type = idTypeName,
                isId = true,
                isNullable = true
            )
            properties["createdAt"] = PropertyInfo(
                name = "createdAt",
                columnName = "created_at",
                type = "java.time.LocalDateTime",
                isId = false,
                isNullable = false
            )
            properties["updatedAt"] = PropertyInfo(
                name = "updatedAt",
                columnName = "updated_at",
                type = "java.time.LocalDateTime",
                isId = false,
                isNullable = false
            )

            // Collect properties declared in the entity class
            classDeclaration.getDeclaredProperties()
                .filter { prop -> hasColumnAnnotation(prop) || hasIdAnnotation(prop) }
                .forEach { prop ->
                    val type = prop.type.resolve()
                    val propName = prop.simpleName.asString()
                    val converter = getConverterInfo(prop)  // converter 정보 가져오기
                    properties[propName] = PropertyInfo(
                        name = propName,
                        columnName = getColumnName(prop),
                        type = type.declaration.qualifiedName?.asString() ?: "kotlin.String",
                        isId = hasIdAnnotation(prop),
                        isNullable = type.isMarkedNullable,
                        hasConverter = converter != null,    // converter 있는지 여부
                        converterType = converter            // converter 타입
                    )
                }

            return properties.values.toList()
        }

        private fun getConverterInfo(property: KSPropertyDeclaration): String? =
            property.annotations
                .find { it.shortName.asString() == "Convert" }
                ?.arguments
                ?.find { it.name?.asString() == "converter" }
                ?.value
                ?.toString()
                ?.replace(".class", "")

        private fun generateMapperClass(
            className: String,
            properties: List<PropertyInfo>,
            idTypeName: String,
            classDeclaration: KSClassDeclaration
        ): String = buildString {
            appendLine("@Component")
            appendLine("class ${className}Mapper : EntityMapper<$className, $idTypeName> {")
            val indentationLevel = 1

            // Table name
            appendLine(
                indent(
                    "override val tableName: String = \"${getTableName(classDeclaration)}\"",
                    indentationLevel
                )
            )

            // All columns
            appendLine(indent("override val columns: List<String> = listOf(", indentationLevel))
            properties.forEach { prop ->
                appendLine(indent("\"${prop.columnName}\",", indentationLevel + 1))
            }
            appendLine(indent(")", indentationLevel))

            // ID column
            appendLine(indent("override val idColumn: String = \"id\"", indentationLevel))

            // System columns
            appendLine(indent("override val systemColumns: Set<String> = setOf(", indentationLevel))
            systemColumns.forEach { col ->
                appendLine(indent("\"$col\",", indentationLevel + 1))
            }
            appendLine(indent(")", indentationLevel))

            // SQL Generator Constants
            val sqlConstants = """
                private fun insertableColumns(): List<String> = columns.filterNot { 
                    it == idColumn || it in systemColumns 
                }
                
                private fun updatableColumns(): List<String> = columns.filterNot { 
                    it == idColumn || it in systemColumns 
                }
                
                private fun upsertColumns(): List<String> = columns.filterNot { 
                    it in systemColumns 
                }
            """.trimIndent()
            appendLine(indent(sqlConstants, indentationLevel))

            // setValues method
            val setValuesMethod = generateSetValuesMethod(className, properties, idTypeName)
            appendLine(indent(setValuesMethod, indentationLevel))

            // fromRow method
            val fromRowMethod = generateFromRowMethod(className, properties, idTypeName)
            appendLine(indent(fromRowMethod, indentationLevel))

            // getIdValue method
            val getIdValueMethod = """
                override fun getIdValue(entity: $className): $idTypeName? = entity.id
            """.trimIndent()
            appendLine(indent(getIdValueMethod, indentationLevel))

            val extractIdMethod = """
                override fun extractId(rs: ResultSet): $idTypeName {
                    return rs.getLong(1) as $idTypeName
                }
            """.trimIndent()
            appendLine(indent(extractIdMethod, indentationLevel))

            // setValuesForInsert method
            val setValuesForInsertMethod = generateSetValuesForInsertMethod(className, properties)
            appendLine(indent(setValuesForInsertMethod, indentationLevel))

            // getInsertParameterCount method
            val getInsertParameterCountMethod = generateGetInsertParameterCountMethod(properties)
            appendLine(indent(getInsertParameterCountMethod, indentationLevel))

            // getColumnCount method
            val getColumnCountMethod = """
                override fun getColumnCount(): Int {
                    return columns.size - systemColumns.size - (if (idColumn in columns) 1 else 0)
                }
            """.trimIndent()
            appendLine(indent(getColumnCountMethod, indentationLevel))

            val extractColumnValueMethod = generateExtractColumnValueMethod(className, properties)
            appendLine(indent(extractColumnValueMethod, indentationLevel))

            appendLine("}")
        }

        private fun generateExtractColumnValueMethod(
            className: String,
            properties: List<PropertyInfo>
        ): String = buildString {
            appendLine("override fun extractColumnValue(entity: $className, columnName: String): Any? {")
            appendLine("    return when (columnName) {")

            // 여기서 systemColumns, ID 컬럼을 포함할지 말지 선택 가능
            properties.forEach { prop ->
                appendLine("        \"${prop.columnName}\" -> entity.${prop.name}")
            }

            // else -> throw or else -> null
            appendLine("        else -> null")
            appendLine("    }")
            appendLine("}")
        }

        private fun generateSetValuesMethod(
            className: String,
            properties: List<PropertyInfo>,
            idTypeName: String
        ): String = buildString {
            appendLine("override fun setValues(ps: PreparedStatement, entity: $className, operation: Operation) {")
            val methodIndentLevel = 1
            appendLine(indent("var index = 1", methodIndentLevel))
            appendLine(indent("when (operation) {", methodIndentLevel))
            val caseIndentLevel = methodIndentLevel + 1

            // INSERT case
            appendLine(indent("Operation.INSERT -> {", caseIndentLevel))
            val bodyIndentLevel = caseIndentLevel + 1
            val propertySettersInsert = generatePropertySetters(
                properties.filterNot { it.isId || it.columnName in systemColumns },
                bodyIndentLevel
            )
            append(propertySettersInsert)
            appendLine(indent("}", caseIndentLevel))

            // UPDATE case
            appendLine(indent("Operation.UPDATE -> {", caseIndentLevel))
            val propertySettersUpdate = generatePropertySetters(
                properties.filterNot { it.isId || it.columnName in systemColumns },
                bodyIndentLevel
            )
            append(propertySettersUpdate)
            appendLine(indent("// ID for WHERE clause", bodyIndentLevel))
            appendLine(indent("ps.setObject(index++, entity.id)", bodyIndentLevel))
            appendLine(indent("}", caseIndentLevel))

            // UPSERT case
            appendLine(indent("Operation.UPSERT -> {", caseIndentLevel))
            appendLine(indent("// UPSERT requires ID", bodyIndentLevel))
            val idPropertySetter = generatePropertySetter(properties.find { it.isId }!!, bodyIndentLevel)
            append(idPropertySetter)
            val propertySettersUpsert = generatePropertySetters(
                properties.filterNot { it.isId || it.columnName in systemColumns },
                bodyIndentLevel
            )
            append(propertySettersUpsert)
            appendLine(indent("}", caseIndentLevel))

            appendLine(indent("}", methodIndentLevel))
            appendLine("}")
        }

        private fun generatePropertySetter(prop: PropertyInfo, indentLevel: Int): String {
            val code = when {
                prop.hasConverter -> """
            entity.${prop.name}?.let { 
                val converter = ${prop.converterType}()
                ps.setString(index++, converter.convertToDatabaseColumn(it))
            } ?: ps.setNull(index++, Types.VARCHAR)
        """.trimIndent()

                prop.type.endsWith("LocalDateTime") ->
                    "entity.${prop.name}?.let { ps.setTimestamp(index++, java.sql.Timestamp.valueOf(it)) } ?: ps.setNull(index++, Types.TIMESTAMP)"

                prop.isEnum() ->
                    "entity.${prop.name}?.let { ps.setString(index++, it.name) } ?: ps.setNull(index++, Types.VARCHAR)"

                prop.isNullable ->
                    "entity.${prop.name}?.let { ps.set${getJdbcType(prop.type)}(index++, it) } ?: ps.setNull(index++, ${
                        getSqlType(
                            prop.type
                        )
                    })"

                else ->
                    "ps.set${getJdbcType(prop.type)}(index++, entity.${prop.name})"
            }
            return indent(code, indentLevel) + "\n"
        }

        private fun generatePropertySetters(properties: List<PropertyInfo>, indentLevel: Int): String {
            return properties.joinToString("") { prop ->
                generatePropertySetter(prop, indentLevel)
            }
        }

        private fun generateFromRowMethod(
            className: String,
            properties: List<PropertyInfo>,
            idTypeName: String
        ): String = buildString {
            appendLine("override fun fromRow(rs: ResultSet): $className {")
            val methodIndentLevel = 1
            appendLine(indent("return $className(", methodIndentLevel))
            val constructorIndentLevel = methodIndentLevel + 1
            val constructorParams = generateConstructorParameters(properties, constructorIndentLevel)
            append(constructorParams)
            appendLine(indent(")", methodIndentLevel))
            appendLine(indent(".apply {", methodIndentLevel))
            val applyIndentLevel = methodIndentLevel + 1
            appendLine(indent("id = rs.getObject(\"id\") as? $idTypeName", applyIndentLevel))
            appendLine(indent("createdAt = rs.getTimestamp(\"created_at\").toLocalDateTime()", applyIndentLevel))
            appendLine(indent("updatedAt = rs.getTimestamp(\"updated_at\").toLocalDateTime()", applyIndentLevel))
            appendLine(indent("}", methodIndentLevel))
            appendLine("}")
        }

        private fun generateConstructorParameters(properties: List<PropertyInfo>, indentLevel: Int): String {
            return properties
                .filterNot { it.columnName in (systemColumns + "id") }
                .joinToString(",\n") { prop ->
                    indent("${prop.name} = ${generateResultSetGetter(prop)}", indentLevel)
                } + "\n"
        }

        private fun generateResultSetGetter(prop: PropertyInfo): String = when {
            prop.hasConverter ->
                """
                ${prop.converterType}().convertToEntityAttribute(
                    rs.getString("${prop.columnName}")?.takeIf { !rs.wasNull() }
                )
                """.trimIndent()

            prop.type.endsWith("LocalDateTime") ->
                "rs.getTimestamp(\"${prop.columnName}\")${if (prop.isNullable) "?" else ""}.toLocalDateTime()"

            prop.isEnum() ->
                "rs.getString(\"${prop.columnName}\")?.let { ${prop.type}.valueOf(it) }" +
                        if (!prop.isNullable) " ?: ${prop.type}.valueOf(\"DEFAULT\")" else ""

            prop.isNullable ->
                "rs.get${getJdbcType(prop.type)}(\"${prop.columnName}\").takeIf { !rs.wasNull() }"

            else ->
                "rs.get${getJdbcType(prop.type)}(\"${prop.columnName}\")"
        }

        private fun generateExtractIdMethod(idTypeName: String): String {
            return """
                override fun extractId(rs: ResultSet): $idTypeName {
                    return when ("$idTypeName") {
                        "kotlin.Int" -> rs.getInt(idColumn) as $idTypeName
                        "kotlin.Long" -> rs.getLong(idColumn) as $idTypeName
                        "kotlin.String" -> rs.getString(idColumn) as $idTypeName
                        else -> throw IllegalStateException("Unsupported ID type: $idTypeName")
                    }
                }
            """.trimIndent()
        }

        private fun generateImports(
            classDeclaration: KSClassDeclaration,
            properties: List<PropertyInfo>,
            idTypeName: String
        ): String {
            val imports = mutableSetOf(
                "com.wildrew.batch.common.repository.mysql.core.interfaces.EntityMapper",
                "com.wildrew.batch.common.repository.mysql.core.interfaces.Operation",
                "com.wildrew.batch.common.converter.StringListConverter",  // 컨버터 임포트 추가
                "org.springframework.stereotype.Component",
                "java.sql.PreparedStatement",
                "java.sql.ResultSet",
                "java.sql.Types",
                "java.time.LocalDateTime",
                classDeclaration.qualifiedName?.asString() ?: ""
            )

            // Add imports for enum types
            properties.filter { it.isEnum() }.forEach { prop ->
                imports.add(prop.type)
            }

            // Add import for idTypeName if necessary
            if (needsImport(idTypeName)) {
                imports.add(idTypeName)
            }

            return imports.filter { it.isNotBlank() }
                .sorted()
                .joinToString("\n") { "import $it" }
        }

        private fun generateSetValuesForInsertMethod(
            className: String,
            properties: List<PropertyInfo>
        ): String = buildString {
            appendLine("override fun setValuesForInsert(ps: PreparedStatement, entity: $className, startIndex: Int) {")
            val methodIndentLevel = 1
            appendLine(indent("var index = startIndex", methodIndentLevel))
            val propertySetters = generatePropertySetters(
                properties.filterNot { it.isId || it.columnName in systemColumns },
                methodIndentLevel
            )
            append(propertySetters)
            appendLine("}")
        }

        private fun generateGetInsertParameterCountMethod(
            properties: List<PropertyInfo>
        ): String = """
            override fun getInsertParameterCount(): Int = ${
            properties.count { !it.isId && it.columnName !in systemColumns }
        }
        """.trimIndent()

        private fun needsImport(typeName: String): Boolean {
            return !typeName.startsWith("kotlin.") &&
                    !typeName.startsWith("java.lang.") &&
                    !typeName.startsWith("java.time.") &&
                    typeName.contains(".")
        }

        private fun getJdbcType(type: String): String = when (type) {
            "kotlin.String" -> "String"
            "kotlin.Int" -> "Int"
            "kotlin.Long" -> "Long"
            "kotlin.Double" -> "Double"
            "kotlin.Float" -> "Float"
            "kotlin.Boolean" -> "Boolean"
            "java.time.LocalDateTime" -> "Timestamp"
            else -> "Object"
        }

        private fun getSqlType(type: String): String = when (type) {
            "kotlin.String" -> "Types.VARCHAR"
            "kotlin.Int" -> "Types.INTEGER"
            "kotlin.Long" -> "Types.BIGINT"
            "kotlin.Double" -> "Types.DOUBLE"
            "kotlin.Float" -> "Types.FLOAT"
            "kotlin.Boolean" -> "Types.BOOLEAN"
            "java.time.LocalDateTime" -> "Types.TIMESTAMP"
            else -> "Types.OTHER"
        }

        private fun hasColumnAnnotation(property: KSPropertyDeclaration): Boolean =
            property.annotations.any {
                it.shortName.asString() == "Column"
            }

        private fun hasIdAnnotation(property: KSPropertyDeclaration): Boolean =
            property.annotations.any { it.shortName.asString() == "Id" }

        private fun getColumnName(property: KSPropertyDeclaration): String =
            property.annotations
                .find { it.shortName.asString() == "Column" }
                ?.arguments
                ?.find { it.name?.asString() == "name" }
                ?.value as? String
                ?: property.simpleName.asString()
                    .replace(Regex("([a-z])([A-Z])"), "$1_$2")
                    .lowercase()

        private fun getTableName(classDeclaration: KSClassDeclaration): String {
            // Check for @Table annotation
            val tableAnnotation = classDeclaration.annotations.find {
                it.shortName.asString() == "Table"
            }

            val tableName = tableAnnotation?.arguments?.find {
                it.name?.asString() == "name"
            }?.value as? String

            // Use @Table(name="...") if available
            if (!tableName.isNullOrBlank()) {
                return tableName
            }

            // Otherwise, generate based on class name
            return classDeclaration.simpleName.asString()
                .replace(Regex("([a-z])([A-Z])"), "$1_$2")
                .lowercase()
        }

        private fun indent(text: String, level: Int): String {
            val indentation = "    ".repeat(level)
            return text.lines().joinToString("\n") { if (it.isNotBlank()) indentation + it else it }
        }
    }

    private data class PropertyInfo(
        val name: String,
        val columnName: String,
        val type: String,
        val isId: Boolean,
        val isNullable: Boolean,
        val hasConverter: Boolean = false,        // converter 여부 추가
        val converterType: String? = null         // converter 타입 추가
    ) {
        fun isEnum(): Boolean = !type.startsWith("kotlin.") &&
                !type.startsWith("java.") &&
                !type.contains("LocalDateTime")
    }
}
