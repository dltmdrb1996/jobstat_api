package com.wildrew.ksp.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.getAnnotationsByType
import jakarta.persistence.*

const val ENTITY_MAPPER_QUALIFIED_NAME = "com.wildrew.jobstat.core.core_jdbc_batch.core.interfaces.EntityMapper"
const val OPERATION_QUALIFIED_NAME = "com.wildrew.jobstat.core.core_jdbc_batch.core.interfaces.Operation"
const val BASE_ENTITY_QUALIFIED_NAME = "com.wildrew.jobstat.core.core_global.model.BaseEntity"
const val AUDITABLE_SNOW_ENTITY_QUALIFIED_NAME = "com.wildrew.jobstat.core.core_jpa_base.base.AuditableEntitySnow"

class KspBatchProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment
    ): SymbolProcessor = KspBatchProcessor(environment)
}

@OptIn(KspExperimental::class)
class KspBatchProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private val logger = environment.logger

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getSymbolsWithAnnotation(Entity::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { classDeclaration ->
                try {
                    if (classDeclaration.getAllSuperTypes().any { it.declaration.qualifiedName?.asString() == BASE_ENTITY_QUALIFIED_NAME }) {
                        generateMapperFile(classDeclaration, environment)
                    } else {
                        logger.info("Skipping ${classDeclaration.qualifiedName?.asString()}: Does not extend $BASE_ENTITY_QUALIFIED_NAME")
                    }
                } catch (e: Exception) {
                    logger.error(
                        "Failed to generate mapper for ${classDeclaration.qualifiedName?.asString()}: ${e.message} \n ${e.stackTraceToString()}",
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
        val entityClassName = classDeclaration.simpleName.asString()
        val mapperClassName = "${entityClassName}GeneratedMapper" // 이름 충돌 방지 및 명시

        val idTypeName = "kotlin.Long"

        val isAuditable = classDeclaration.getAllSuperTypes().any { it.declaration.qualifiedName?.asString() == AUDITABLE_SNOW_ENTITY_QUALIFIED_NAME }
        val properties = collectProperties(classDeclaration, isAuditable)

        val fileContent = buildString {
            append("package $packageName\n\n") // 생성될 파일의 패키지
            append(generateImports(classDeclaration, properties, idTypeName, isAuditable))
            append("\n\n")
            append(generateMapperClass(entityClassName, mapperClassName, properties, idTypeName, classDeclaration, isAuditable))
        }

        environment.codeGenerator.createNewFile(
            dependencies = Dependencies(true, classDeclaration.containingFile!!),
            packageName = packageName,
            fileName = mapperClassName
        ).use {
            it.write(fileContent.toByteArray())
        }
        logger.info("Generated mapper: $packageName.$mapperClassName for $entityClassName")
    }

    @OptIn(KspExperimental::class)
    private fun collectProperties(
        classDeclaration: KSClassDeclaration,
        isAuditableEntity: Boolean // BaseAutoIncEntity 또는 AuditableEntitySnow 인지 여부
    ): List<PropertyInfo> {
        val propertiesMap = mutableMapOf<String, PropertyInfo>()

        // 1. 모든 상위 타입(슈퍼 클래스 및 인터페이스)을 포함하여 프로퍼티 수집
        val allPropertiesInHierarchy = mutableListOf<KSPropertyDeclaration>()
        var currentClass: KSClassDeclaration? = classDeclaration
        val visitedSupertypes = mutableSetOf<String>() // 무한 루프 방지

        // 현재 클래스의 프로퍼티 추가
        allPropertiesInHierarchy.addAll(currentClass!!.getDeclaredProperties())

        // 상위 클래스들의 프로퍼티 추가 (MappedSuperclass 포함)
        fun collectSuperClassProperties(clazz: KSClassDeclaration) {
            clazz.getAllSuperTypes().forEach { superType ->
                val superDeclaration = superType.declaration
                if (superDeclaration is KSClassDeclaration &&
                    superDeclaration.qualifiedName?.asString() !in visitedSupertypes &&
                    (superDeclaration.classKind == ClassKind.CLASS || superDeclaration.classKind == ClassKind.INTERFACE || // 인터페이스는 프로퍼티를 직접 갖지 않지만, KSPropertyDeclaration은 가질 수 있음
                            superDeclaration.getAnnotationsByType(MappedSuperclass::class).any()) // MappedSuperclass의 프로퍼티도 포함
                ) {
                    visitedSupertypes.add(superDeclaration.qualifiedName!!.asString())
                    allPropertiesInHierarchy.addAll(superDeclaration.getDeclaredProperties())
                    collectSuperClassProperties(superDeclaration) // 재귀적으로 상위 클래스 탐색
                }
            }
        }
        collectSuperClassProperties(classDeclaration)


        // 2. 수집된 모든 프로퍼티를 기반으로 PropertyInfo 생성
        allPropertiesInHierarchy
            .distinctBy { it.simpleName.asString() } // 이름 중복 제거 (하위 클래스가 오버라이드한 경우 하위 클래스 우선)
            .filter { prop ->
                // @Transient 어노테이션이 붙은 필드는 제외
                !prop.getAnnotationsByType(Transient::class).any() &&
                        // JPA @Column, @Id, @Convert 어노테이션이 있거나,
                        // 또는 BaseEntity/Auditable 계열의 기본 필드(id, createdAt, updatedAt)인 경우
                        (prop.getAnnotationsByType(Column::class).any() ||
                                prop.getAnnotationsByType(Id::class).any() ||
                                prop.getAnnotationsByType(Convert::class).any() ||
                                // BaseEntity, BaseAutoIncEntity, AuditableEntitySnow 에 정의된 필드명들
                                prop.simpleName.asString() in listOf("id", "createdAt", "updatedAt", "version", "_deleted", "_deletedAt", "isDeleted", "deletedAt")) // SoftDelete, Versioned 필드도 고려
            }
            .forEach { prop ->
                val type = prop.type.resolve()
                val propName = prop.simpleName.asString()

                // 이미 추가된 프로퍼티는 건너뜀 (예: 하위 클래스에서 오버라이드)
                if (propertiesMap.containsKey(propName)) return@forEach

                var converterQualifiedName: String? = null
                val convertAnnotationInstance: Convert? = prop.getAnnotationsByType(Convert::class).firstOrNull()
                if (convertAnnotationInstance != null) {
                    prop.annotations.filter { it.shortName.asString() == "Convert" }.firstOrNull()?.let { an ->
                        an.arguments.find { it.name?.asString() == "converter" }?.value?.let { converterValue ->
                            if (converterValue is KSType) {
                                val fqn = converterValue.declaration.qualifiedName?.asString()
                                if (fqn != "java.lang.Void") { // 기본값인 Void.class 제외
                                    converterQualifiedName = fqn
                                }
                            }
                        }
                    }
                }

                val isBaseField = classDeclaration.getAllSuperTypes().any { superType ->
                    superType.declaration.qualifiedName?.asString()?.startsWith("com.wildrew.jobstat.core.core_jpa_base.base") == true &&
                            (superType.declaration as KSClassDeclaration).getDeclaredProperties().any { it.simpleName.asString() == propName }
                } || prop.parentDeclaration?.qualifiedName?.asString()?.startsWith("com.wildrew.jobstat.core.core_jpa_base.base") == true


                propertiesMap[propName] = PropertyInfo(
                    name = propName,
                    columnName = getColumnName(prop), // @Column(name=) 우선, 없으면 스네이크 케이스 변환
                    type = type.declaration.qualifiedName?.asString() ?: "kotlin.Any",
                    isId = prop.getAnnotationsByType(Id::class).any() || (propName == "id" && isBaseField), // @Id 어노테이션 또는 베이스 엔티티의 'id'
                    isNullable = type.isMarkedNullable,
                    isEnum = type.declaration is KSClassDeclaration && (type.declaration as KSClassDeclaration).classKind == ClassKind.ENUM_CLASS,
                    hasConverter = converterQualifiedName != null,
                    converterType = converterQualifiedName,
                    isBaseField = isBaseField
                )
            }

        // 정렬: ID 필드를 맨 앞으로, 그 다음은 이름순 (선택적)
        return propertiesMap.values.toList().sortedWith(compareBy({ !it.isId }, { it.name }))
    }


    private fun getColumnName(property: KSPropertyDeclaration): String =
        property.getAnnotationsByType(Column::class).firstOrNull()?.name?.takeIf { it.isNotBlank() }
            ?: property.simpleName.asString().replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()

    private fun getTableName(classDeclaration: KSClassDeclaration): String =
        classDeclaration.getAnnotationsByType(Table::class).firstOrNull()?.name?.takeIf { it.isNotBlank() }
            ?: classDeclaration.simpleName.asString().replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()


    private fun generateImports(
        classDeclaration: KSClassDeclaration,
        properties: List<PropertyInfo>,
        idTypeName: String, // 이제 항상 "kotlin.Long"
        isAuditable: Boolean
    ): String {
        val imports = mutableSetOf<String>()
        imports.add(ENTITY_MAPPER_QUALIFIED_NAME)
        imports.add(OPERATION_QUALIFIED_NAME)
        imports.add("org.springframework.stereotype.Component") // @Component
        imports.add("java.sql.PreparedStatement")
        imports.add("java.sql.ResultSet")
        imports.add("java.sql.Types")
        imports.add(classDeclaration.qualifiedName!!.asString()) // 엔티티 클래스 자체

        if (properties.any { it.type == "java.time.LocalDateTime" } || isAuditable) {
            imports.add("java.time.LocalDateTime")
        }

        properties.forEach { prop ->
            if (prop.isEnum && prop.type.contains(".")) imports.add(prop.type) // Enum 타입 import
            if (prop.hasConverter && prop.converterType != null && prop.converterType.contains(".")) imports.add(prop.converterType) // Converter 타입 import
        }

        return imports.filter { it.isNotBlank() }.sorted().joinToString("\n") { "import $it" }
    }


    private fun generateMapperClass(
        entityClassName: String,
        mapperClassName: String,
        properties: List<PropertyInfo>,
        idTypeName: String, // 이제 항상 "kotlin.Long"
        classDeclaration: KSClassDeclaration,
        isAuditable: Boolean
    ): String = buildString {
        appendLine("@Component")
        appendLine("class $mapperClassName : EntityMapper<$entityClassName, $idTypeName> {") // ID 타입을 Long으로 명시

        val indentLevel = 1
        // tableName
        appendLine(indent("override val tableName: String = \"${getTableName(classDeclaration)}\"", indentLevel))
        // columns
        appendLine(indent("override val columns: List<String> = listOf(", indentLevel))
        properties.forEach { prop ->
            appendLine(indent("\"${prop.columnName}\",", indentLevel + 1))
        }
        appendLine(indent(")", indentLevel))
        // idColumn
        appendLine(indent("override val idColumn: String = \"id\"", indentLevel))
        // systemColumns (Auditable 여부에 따라 동적으로 설정)
        val systemCols = if (isAuditable) "setOf(\"created_at\", \"updated_at\")" else "emptySet()"
        appendLine(indent("override val systemColumns: Set<String> = $systemCols", indentLevel))

        // fromRow
        appendLine(indent(generateFromRowMethod(entityClassName, properties, classDeclaration, isAuditable), indentLevel))
        // setValues
        appendLine(indent(generateSetValuesMethod(entityClassName, properties, isAuditable), indentLevel))
        // getIdValue
        appendLine(indent("override fun getIdValue(entity: $entityClassName): $idTypeName = entity.id", indentLevel)) // BaseEntity의 id는 non-null Long
        // extractId (ResultSet)
        appendLine(indent("override fun extractId(rs: ResultSet): $idTypeName = rs.getLong(idColumn)", indentLevel)) // idColumn 사용
        // setValuesForInsert
        appendLine(indent(generateSetValuesForInsertMethod(entityClassName, properties, isAuditable), indentLevel))
        // getInsertParameterCount
        val insertableProps = properties.filterNot { it.isId || (isAuditable && (it.name == "createdAt" || it.name == "updatedAt")) }
        appendLine(indent("override fun getInsertParameterCount(): Int = ${insertableProps.size}", indentLevel))
        // getColumnCount
        val columnCountSystemCols = if (isAuditable) 2 else 0
        appendLine(indent("override fun getColumnCount(): Int = columns.size - $columnCountSystemCols - 1 // id 제외", indentLevel))
        // extractColumnValue
        appendLine(indent(generateExtractColumnValueMethod(entityClassName, properties), indentLevel))

        appendLine("}")
    }

    private fun generateFromRowMethod(
        entityClassName: String,
        allCollectedProperties: List<PropertyInfo>, // 모든 프로퍼티 정보 (베이스 + 직접 선언)
        classDeclaration: KSClassDeclaration,
        isAuditable: Boolean // isAuditable은 이제 allCollectedProperties에서 판단 가능
    ): String = buildString {
        appendLine("override fun fromRow(rs: ResultSet): $entityClassName {")
        val constructor = classDeclaration.primaryConstructor
            ?: run {
                logger.error("No primary constructor found for $entityClassName", classDeclaration)
                appendLine(indent("throw IllegalStateException(\"No primary constructor found for $entityClassName\")", 1))
                appendLine("}")
                return@buildString
            }

        // 생성자 파라미터 이름들
        val constructorParamNames = constructor.parameters.mapNotNull { it.name?.asString() }.toSet()

        // 1. 생성자로 엔티티 인스턴스 생성
        appendLine(indent("val entity = $entityClassName(", 1))
        constructor.parameters.forEachIndexed { index, param ->
            val paramName = param.name!!.asString()
            val propInfo = allCollectedProperties.find { it.name == paramName }

            if (propInfo == null) {
                val paramType = param.type.resolve()
                val defaultValue = getDefaultValueForType(paramType, paramName)
                logger.warn("Property info not found for constructor parameter '$paramName' in $entityClassName. Using default: $defaultValue.", classDeclaration)
                append(indent("$paramName = $defaultValue", 2))
            } else {
                append(indent("$paramName = ${generateResultSetGetter(propInfo, "rs")}", 2))
            }
            if (index < constructor.parameters.size - 1) {
                append(",")
            }
            appendLine()
        }
        appendLine(indent(")", 1))

        // 2. 생성자에서 처리되지 않은 'var' 프로퍼티들을 apply 블록에서 설정
        //    (id, createdAt, updatedAt 및 엔티티에 직접 선언된 var 필드들)
        appendLine(indent(".apply {", 1))
        allCollectedProperties.forEach { propInfo ->
            // 생성자에서 이미 처리된 프로퍼티는 건너<0xE1><0x8A><0x9C>
            if (propInfo.name in constructorParamNames) {
                // 이미 생성자에서 처리됨.
                // 단, 생성자 파라미터와 이름은 같지만 타입이 다르거나 하는 예외 케이스가 있다면 추가 로직 필요
            } else {
                // 생성자에서 처리되지 않은 프로퍼티들 (주로 상속받은 var 필드 또는 엔티티 내 var 필드)
                // 해당 필드가 'var'이고 접근 가능해야 함.
                // BaseSnowIdEntity의 id는 val이므로 여기서는 설정 불가 (생성자로 주입되어야 함)
                // BaseAutoIncEntity/AuditableEntitySnow의 createdAt, updatedAt은 var protected set이므로,
                // KSP 생성 코드가 같은 패키지라면 접근 가능.
                // Board의 viewCount, likeCount 등은 var public set이므로 접근 가능.

                val propertyDeclaration = classDeclaration.getAllProperties().find { it.simpleName.asString() == propInfo.name }
                val isVarProperty = propertyDeclaration?.isMutable == true

                if (isVarProperty) {
                    // 'id'는 BaseSnowIdEntity에서 val이므로, 여기서 설정하면 컴파일 오류 발생.
                    // BaseAutoIncEntity의 id도 val.
                    // 이들은 생성자에서 처리되거나, KSP가 생성자에 id를 포함하도록 유도해야 함.
                    // 현재 코드는 BaseEntity의 id를 val로 가정하고, 이 id를 설정하는 로직은 없음.
                    // 만약 id가 var이라면 아래 코드가 유효.
                    // if (propInfo.isId) {
                    //    appendLine(indent("this.${propInfo.name} = rs.getLong(idColumn) // Assuming id is var", 2))
                    // } else { ... }

                    // id는 val 이므로 여기서 설정하지 않음. 생성자에서 처리되거나, 엔티티 구조 변경 필요
                    if (!propInfo.isId) { // id가 아닌 var 프로퍼티들
                        appendLine(indent("this.${propInfo.name} = ${generateResultSetGetter(propInfo, "rs")}", 2))
                    }
                } else if (propInfo.name == "id") {
                    // id는 val이므로, 생성자에서 초기화 되어야 함.
                    // 만약 fromRow에서 id를 설정해야 한다면, 엔티티의 id가 var이거나, id를 받는 생성자를 사용해야 함.
                    // 현재 KSP는 엔티티의 기본 생성자를 사용하므로, val id는 rs 값으로 덮어쓸 수 없음.
                    // logger.info("Property '${propInfo.name}' in $entityClassName is val, cannot set from ResultSet in apply block unless it's a constructor param.")
                }
            }
        }
        appendLine(indent("}", 1))
        appendLine(indent("return entity",1)) // apply된 entity 반환
        appendLine("}")
    }

    // getDefaultValueForType 헬퍼 함수 추가
    private fun getDefaultValueForType(paramType: KSType, paramName: String): String {
        return when (paramType.declaration.qualifiedName?.asString()) {
            "kotlin.String" -> if (paramType.isMarkedNullable) "null" else "\"\"" // 빈 문자열 또는 예외 발생
            "kotlin.Int" -> if (paramType.isMarkedNullable) "null" else "0"
            "kotlin.Long" -> if (paramType.isMarkedNullable) "null" else "0L"
            "kotlin.Double" -> if (paramType.isMarkedNullable) "null" else "0.0"
            "kotlin.Float" -> if (paramType.isMarkedNullable) "null" else "0.0f"
            "kotlin.Boolean" -> if (paramType.isMarkedNullable) "null" else "false"
            "java.time.LocalDateTime" -> if (paramType.isMarkedNullable) "null" else "LocalDateTime.now() /* TODO: Review default for LocalDateTime */"
            else -> {
                if (paramType.isMarkedNullable) {
                    "null"
                } else if (paramType.declaration is KSClassDeclaration && (paramType.declaration as KSClassDeclaration).classKind == ClassKind.ENUM_CLASS) {
                    // Enum의 경우 첫 번째 값을 기본값으로 사용하거나, nullable로 처리
                    val enumClass = paramType.declaration as KSClassDeclaration
                    val firstEnumValue = enumClass.declarations.filterIsInstance<KSClassDeclaration>()
                        .firstOrNull { it.classKind == ClassKind.ENUM_ENTRY }?.simpleName?.asString()
                    if (firstEnumValue != null) {
                        "${enumClass.qualifiedName?.asString()}.$firstEnumValue"
                    } else {
                        "TODO(\"Default enum value for $paramName of type ${paramType.declaration.qualifiedName?.asString()}\")"
                    }
                } else if (paramType.declaration.qualifiedName?.asString()?.startsWith("kotlin.collections.") == true) {
                    // 컬렉션 타입 (List, Set, Map 등). 빈 컬렉션 또는 null (nullable인 경우)
                    when(paramType.declaration.qualifiedName?.asString()){
                        "kotlin.collections.List" -> "emptyList()"
                        "kotlin.collections.Set" -> "emptySet()"
                        "kotlin.collections.Map" -> "emptyMap()"
                        else -> "null /* TODO: Review default for Collection */"
                    }
                }
                else {
                    "TODO(\"Default value for non-nullable $paramName of type ${paramType.declaration.qualifiedName?.asString()}\")"
                }
            }
        }
    }

    private fun generateResultSetGetter(prop: PropertyInfo, rsVarName: String): String {
        val getterPrefix = when (prop.type) {
            "kotlin.String" -> "$rsVarName.getString(\"${prop.columnName}\")"
            "kotlin.Int" -> "$rsVarName.getInt(\"${prop.columnName}\")"
            "kotlin.Long" -> "$rsVarName.getLong(\"${prop.columnName}\")"
            "kotlin.Boolean" -> "$rsVarName.getBoolean(\"${prop.columnName}\")"
            "java.time.LocalDateTime" -> "$rsVarName.getTimestamp(\"${prop.columnName}\")?.toLocalDateTime()" // Nullable 처리
            else -> { // Enum 또는 Converter 또는 기타 타입
                if (prop.isEnum) {
                    "$rsVarName.getString(\"${prop.columnName}\")?.let { ${prop.type}.valueOf(it) }"
                } else if (prop.hasConverter && prop.converterType != null) {
                    "(${prop.converterType}()).convertToEntityAttribute($rsVarName.getString(\"${prop.columnName}\"))"
                } else {
                    "$rsVarName.getObject(\"${prop.columnName}\") as? ${prop.type.substringAfterLast('.')}" // 단순 캐스팅 (위험할 수 있음)
                }
            }
        }
        return if (prop.isNullable && !prop.type.endsWith("LocalDateTime") && !prop.isEnum && !prop.hasConverter) { // LocalDateTime, Enum, Converter는 이미 nullable 처리
            "$getterPrefix.takeIf { !$rsVarName.wasNull() }"
        } else {
            getterPrefix
        }
    }

    private fun generateSetValuesMethod(
        entityClassName: String,
        properties: List<PropertyInfo>,
        isAuditable: Boolean
    ): String = buildString {
        appendLine("override fun setValues(ps: PreparedStatement, entity: $entityClassName, operation: Operation) {")
        appendLine(indent("var index = 1", 1))
        // INSERT, UPDATE 공통 필드 (ID, Auditable 필드 제외)
        properties.filterNot { it.isId || (isAuditable && (it.name == "createdAt" || it.name == "updatedAt")) }
            .forEach { prop ->
                appendLine(indent(generatePreparedStatementSetter(prop, "entity", "ps", "index++"), 1))
            }

        appendLine(indent("if (operation == Operation.UPDATE) {", 1))
        appendLine(indent("ps.setLong(index++, entity.id)", 2)) // WHERE id = ?
        appendLine(indent("}", 1))
        appendLine("}")
    }

    private fun generateSetValuesForInsertMethod(
        entityClassName: String,
        properties: List<PropertyInfo>,
        isAuditable: Boolean
    ): String = buildString {
        appendLine("override fun setValuesForInsert(ps: PreparedStatement, entity: $entityClassName, startIndex: Int) {")
        appendLine(indent("var index = startIndex", 1))
        // INSERT 시 ID와 Auditable 필드는 SQL 생성기가 처리하거나 DB에서 자동 생성되므로 제외
        properties.filterNot { it.isId || (isAuditable && (it.name == "createdAt" || it.name == "updatedAt")) }
            .forEach { prop ->
                appendLine(indent(generatePreparedStatementSetter(prop, "entity", "ps", "index++"), 1))
            }
        appendLine("}")
    }


    private fun generatePreparedStatementSetter(prop: PropertyInfo, entityVarName: String, psVarName: String, indexVarName: String): String {
        val valueAccess = "$entityVarName.${prop.name}" // 예: entity.userId

        // coreSetter를 생성할 때는 valueAccess 그대로 사용
        val coreSetterLogic = when (prop.type) {
            "kotlin.String" -> "$psVarName.setString($indexVarName, $valueAccess)"
            "kotlin.Int" -> "$psVarName.setInt($indexVarName, $valueAccess)"
            "kotlin.Long" -> "$psVarName.setLong($indexVarName, $valueAccess)"
            "kotlin.Boolean" -> "$psVarName.setBoolean($indexVarName, $valueAccess)"
            "java.time.LocalDateTime" -> "$psVarName.setTimestamp($indexVarName, java.sql.Timestamp.valueOf($valueAccess))"
            else -> {
                if (prop.isEnum) {
                    "$psVarName.setString($indexVarName, $valueAccess.name)"
                } else if (prop.hasConverter && prop.converterType != null) {
                    // 컨버터 사용 시, 컨버터 메소드 호출 결과가 nullable일 수 있으므로,
                    // 컨버터 메소드 자체가 null을 반환하면 setNull을 호출하도록 로직이 구성되어야 함.
                    // 여기서는 일단 단순하게 문자열로 변환한다고 가정.
                    // 실제로는 convertToDatabaseColumn의 반환 타입과 nullability를 고려해야 함.
                    "$psVarName.setString($indexVarName, (${prop.converterType}()).convertToDatabaseColumn($valueAccess))"
                } else {
                    "$psVarName.setObject($indexVarName, $valueAccess)" // 일반 Object 처리
                }
            }
        }

        return if (prop.isNullable) {
            // if 블록 안에서는 valueAccess가 null이 아님이 보장되므로, coreSetterLogic을 사용할 때 valueAccess에 '!!'를 추가
            // 또는 coreSetterLogic 문자열 자체를 valueAccess!!를 사용하도록 수정
            val nonNullValueAccess = "$valueAccess!!" // 예: entity.userId!!
            val coreSetterWithNonNullAssertion = when (prop.type) {
                "kotlin.String" -> "$psVarName.setString($indexVarName, $nonNullValueAccess)"
                "kotlin.Int" -> "$psVarName.setInt($indexVarName, $nonNullValueAccess)"
                "kotlin.Long" -> "$psVarName.setLong($indexVarName, $nonNullValueAccess)"
                "kotlin.Boolean" -> "$psVarName.setBoolean($indexVarName, $nonNullValueAccess)"
                "java.time.LocalDateTime" -> "$psVarName.setTimestamp($indexVarName, java.sql.Timestamp.valueOf($nonNullValueAccess))"
                else -> {
                    if (prop.isEnum) {
                        "$psVarName.setString($indexVarName, $nonNullValueAccess.name)"
                    } else if (prop.hasConverter && prop.converterType != null) {
                        "$psVarName.setString($indexVarName, (${prop.converterType}()).convertToDatabaseColumn($nonNullValueAccess))"
                    } else {
                        "$psVarName.setObject($indexVarName, $nonNullValueAccess)"
                    }
                }
            }

            """
        if ($valueAccess != null) {
            ${indent(coreSetterWithNonNullAssertion, 1)}
        } else {
            ${indent("$psVarName.setNull($indexVarName, ${getSqlTypes(prop.type)})",1)}
        }
        """.trimIndent()
        } else {
            // non-nullable 필드는 null 체크 없이 바로 coreSetterLogic 사용
            coreSetterLogic
        }
    }

    private fun getSqlTypes(type: String): String = when (type) {
        "kotlin.String" -> "Types.VARCHAR"
        "kotlin.Int" -> "Types.INTEGER"
        "kotlin.Long" -> "Types.BIGINT"
        "kotlin.Boolean" -> "Types.BOOLEAN"
        "java.time.LocalDateTime" -> "Types.TIMESTAMP"
        else -> "Types.OTHER" // Enum, Converter는 보통 VARCHAR로 저장
    }

    private fun generateExtractColumnValueMethod(entityClassName: String, properties: List<PropertyInfo>): String = buildString {
        appendLine("override fun extractColumnValue(entity: $entityClassName, columnName: String): Any? {")
        appendLine(indent("return when (columnName) {", 1))
        properties.forEach { prop ->
            appendLine(indent("\"${prop.columnName}\" -> entity.${prop.name}", 2))
        }
        appendLine(indent("else -> null", 2))
        appendLine(indent("}", 1))
        appendLine("}")
    }


    private fun KSPropertyDeclaration.findAnnotation(annotationClass: kotlin.reflect.KClass<out Annotation>): KSAnnotation? {
        return annotations.find { it.shortName.asString() == annotationClass.simpleName && it.annotationType.resolve().declaration.qualifiedName?.asString() == annotationClass.qualifiedName }
    }

    private fun indent(text: String, level: Int, indentation: String = "    "): String {
        val prefix = indentation.repeat(level)
        return text.lines().joinToString("\n") { if (it.isNotBlank()) prefix + it else it }
    }

    data class PropertyInfo(
        val name: String, // 엔티티의 프로퍼티 이름
        val columnName: String, // DB 컬럼 이름
        val type: String, // 프로퍼티의 정규화된 타입 이름
        val isId: Boolean,
        val isNullable: Boolean,
        val isEnum: Boolean,
        val hasConverter: Boolean,
        val converterType: String?,
        val isBaseField: Boolean // BaseEntity 또는 AuditableEntitySnow 에서 온 필드인지 여부
    )
}