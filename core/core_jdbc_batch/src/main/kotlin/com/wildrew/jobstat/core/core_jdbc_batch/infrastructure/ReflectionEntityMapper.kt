// package com.wildrew.jobstat.core.core_jdbc_batch.infrastructure
//
// import com.wildrew.jobstat.core.core_global.model.BaseEntity
// import com.wildrew.jobstat.core.core_jdbc_batch.core.interfaces.EntityMapper
// import com.wildrew.jobstat.core.core_jdbc_batch.core.interfaces.Operation
// import jakarta.persistence.Column
// import org.slf4j.LoggerFactory
// import org.springframework.stereotype.Component
// import java.sql.PreparedStatement
// import java.sql.ResultSet
// import java.sql.Types
// import java.time.LocalDateTime
// import kotlin.reflect.*
// import kotlin.reflect.full.*
//
// @Component
// class EntityMapperFactory {
//    private val mapperCache = mutableMapOf<KClass<*>, EntityMapper<*, *>>()
//
//    @Suppress("UNCHECKED_CAST")
//    fun <T : BaseEntity<ID>, ID : Any> createMapper(entityClass: KClass<T>): EntityMapper<T, ID> =
//        mapperCache.getOrPut(entityClass) {
//            OptimizedReflectionMapper(entityClass)
//        } as EntityMapper<T, ID>
// }
//
// class OptimizedReflectionMapper<T : BaseEntity<ID>, ID : Any>(
//    private val entityClass: KClass<T>,
// ) : EntityMapper<T, ID> {
//    private val logger = LoggerFactory.getLogger(this::class.java)
//
//    // 기본 메타데이터 캐싱
//    private val constructorCache: Map<String, ConstructorMetadata<T>> = initializeConstructorCache()
//    private val propertyGetters: Map<String, PropertyAccessor<T>> = initializePropertyGetters()
//    private val propertySetters: Map<String, PropertyAccessor<T>> = initializePropertySetters()
//
//    // 컬럼 정보 캐싱
//    override val tableName: String = resolveTableName()
//    override val idColumn: String = "id"
//    override val systemColumns: Set<String> = setOf("created_at", "updated_at")
//    override val columns: List<String> = initializeColumns()
//
//    // 메타데이터 클래스들
//    private data class ConstructorMetadata<T>(
//        val constructor: KFunction<T>,
//        val parameters: List<ParameterMetadata>,
//    )
//
//    private data class ParameterMetadata(
//        val name: String,
//        val type: KClass<*>,
//        val isNullable: Boolean,
//        val isEnum: Boolean,
//        val columnName: String,
//    )
//
//    private data class PropertyAccessor<T>(
//        val getter: KProperty1.Getter<T, *>,
//        val type: KClass<*>,
//        val isNullable: Boolean,
//        val isEnum: Boolean,
//        val columnName: String,
//        val jdbcType: Int,
//    )
//
//    private fun initializeConstructorCache(): Map<String, ConstructorMetadata<T>> {
//        val constructor =
//            entityClass.primaryConstructor
//                ?: throw IllegalStateException("Entity must have primary constructor")
//
//        val parameters =
//            constructor.parameters.map { param ->
//                val property = entityClass.memberProperties.first { it.name == param.name }
//                val column = property.findAnnotation<Column>()
//                val columnName =
//                    column?.name?.takeIf { it.isNotBlank() }
//                        ?: param.name!!.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
//
//                ParameterMetadata(
//                    name = param.name!!,
//                    type = param.type.classifier as KClass<*>,
//                    isNullable = param.type.isMarkedNullable,
//                    isEnum = (param.type.classifier as KClass<*>).java.isEnum,
//                    columnName = columnName,
//                )
//            }
//
//        return mapOf("primary" to ConstructorMetadata(constructor, parameters))
//    }
//
//    private fun initializePropertyGetters(): Map<String, PropertyAccessor<T>> =
//        entityClass.memberProperties
//            .filterIsInstance<KProperty1<T, *>>()
//            .associate { prop ->
//                val columnName =
//                    prop.findAnnotation<Column>()?.name
//                        ?: prop.name.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
//
//                columnName to
//                    PropertyAccessor(
//                        getter = prop.getter,
//                        type = prop.returnType.classifier as KClass<*>,
//                        isNullable = prop.returnType.isMarkedNullable,
//                        isEnum = (prop.returnType.classifier as KClass<*>).java.isEnum,
//                        columnName = columnName,
//                        jdbcType = getJdbcType(prop.returnType.classifier as KClass<*>),
//                    )
//            }
//
//    private fun initializePropertySetters(): Map<String, PropertyAccessor<T>> =
//        entityClass.memberProperties
//            .filterIsInstance<KMutableProperty1<T, *>>()
//            .associate { prop ->
//                val columnName =
//                    prop.findAnnotation<Column>()?.name
//                        ?: prop.name.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
//
//                columnName to
//                    PropertyAccessor(
//                        getter = prop.getter,
//                        type = prop.returnType.classifier as KClass<*>,
//                        isNullable = prop.returnType.isMarkedNullable,
//                        isEnum = (prop.returnType.classifier as KClass<*>).java.isEnum,
//                        columnName = columnName,
//                        jdbcType = getJdbcType(prop.returnType.classifier as KClass<*>),
//                    )
//            }
//
//    private fun initializeColumns(): List<String> {
//        val entityColumns =
//            propertyGetters.values
//                .filter { !it.columnName.equals(idColumn, ignoreCase = true) }
//                .map { it.columnName }
//                .sorted()
//        return listOf(idColumn) + systemColumns + entityColumns
//    }
//
//    private fun resolveTableName(): String {
//        val tableAnnotation = entityClass.findAnnotation<jakarta.persistence.Table>()
//        return tableAnnotation?.name?.takeIf { it.isNotBlank() }
//            ?: entityClass.simpleName!!.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
//    }
//
//    override fun fromRow(rs: ResultSet): T {
//        val constructorMetadata = constructorCache["primary"]!!
//        val args = mutableMapOf<KParameter, Any?>()
//
//        constructorMetadata.parameters.forEach { param ->
//            val value =
//                when {
//                    param.isEnum -> {
//                        val enumValue = rs.getString(param.columnName)
//                        if (!rs.wasNull()) {
//                            (param.type.java.enumConstants as Array<Enum<*>>)
//                                .first { it.name == enumValue }
//                        } else {
//                            null
//                        }
//                    }
//                    param.type == LocalDateTime::class ->
//                        rs.getTimestamp(param.columnName)?.toLocalDateTime()
//                    else -> getTypedValue(rs, param.columnName, param.type, param.isNullable)
//                }
//            val constructorParam =
//                constructorMetadata.constructor.parameters
//                    .first { it.name == param.name }
//            args[constructorParam] = value
//        }
//
//        return constructorMetadata.constructor.callBy(args).apply {
//            @Suppress("UNCHECKED_CAST")
//            id = rs.getObject(idColumn) as? ID
//            createdAt = rs.getTimestamp("created_at").toLocalDateTime()
//            updatedAt = rs.getTimestamp("updated_at").toLocalDateTime()
//        }
//    }
//
//    override fun extractId(rs: ResultSet): Long = rs.getLong(idColumn)
//
//    override fun setValues(
//        ps: PreparedStatement,
//        entity: T,
//        operation: Operation,
//    ) {
//        var index = 1
//
//        when (operation) {
//            Operation.INSERT -> {
//                columns
//                    .filterNot { it == idColumn || it in systemColumns }
//                    .forEach { columnName ->
//                        setTypedValue(ps, index++, entity, columnName)
//                    }
//            }
//            Operation.UPDATE -> {
//                columns
//                    .filterNot { it == idColumn || it in systemColumns }
//                    .forEach { columnName ->
//                        setTypedValue(ps, index++, entity, columnName)
//                    }
//                ps.setObject(index, getIdValue(entity))
//            }
//            Operation.UPSERT -> {
//                setTypedValue(ps, index++, entity, idColumn)
//                columns
//                    .filterNot { it == idColumn || it in systemColumns }
//                    .forEach { columnName ->
//                        setTypedValue(ps, index++, entity, columnName)
//                    }
//            }
//        }
//    }
//
//    override fun getIdValue(entity: T): ID? = entity.id
//
//    override fun setValuesForInsert(
//        ps: PreparedStatement,
//        entity: T,
//        startIndex: Int,
//    ) {
//        var index = startIndex
//        columns
//            .filterNot { it == idColumn || it in systemColumns }
//            .forEach { columnName ->
//                setTypedValue(ps, index++, entity, columnName)
//            }
//    }
//
//    override fun getInsertParameterCount(): Int = columns.count { it != idColumn && it !in systemColumns }
//
//    override fun getColumnCount(): Int = columns.size - systemColumns.size - (if (idColumn in columns) 1 else 0)
//
//    override fun extractColumnValue(
//        entity: T,
//        columnName: String,
//    ): Any? {
//        TODO("Not yet implemented")
//    }
//
//    private fun setTypedValue(
//        ps: PreparedStatement,
//        index: Int,
//        entity: T,
//        columnName: String,
//    ) {
//        val accessor = propertyGetters[columnName] ?: return
//        val value = accessor.getter.call(entity)
//
//        when {
//            value == null -> ps.setNull(index, accessor.jdbcType)
//            accessor.isEnum -> ps.setString(index, (value as Enum<*>).name)
//            accessor.type == LocalDateTime::class ->
//                ps.setTimestamp(index, java.sql.Timestamp.valueOf(value as LocalDateTime))
//            else ->
//                when (accessor.type) {
//                    String::class -> ps.setString(index, value as String)
//                    Int::class -> ps.setInt(index, value as Int)
//                    Long::class -> ps.setLong(index, value as Long)
//                    Double::class -> ps.setDouble(index, value as Double)
//                    Boolean::class -> ps.setBoolean(index, value as Boolean)
//                    else -> ps.setObject(index, value)
//                }
//        }
//    }
//
//    private fun getTypedValue(
//        rs: ResultSet,
//        columnName: String,
//        type: KClass<*>,
//        isNullable: Boolean,
//    ): Any? =
//        when (type) {
//            String::class -> rs.getString(columnName)
//            Int::class -> rs.getInt(columnName).let { if (rs.wasNull() && isNullable) null else it }
//            Long::class -> rs.getLong(columnName).let { if (rs.wasNull() && isNullable) null else it }
//            Double::class -> rs.getDouble(columnName).let { if (rs.wasNull() && isNullable) null else it }
//            Boolean::class -> rs.getBoolean(columnName).let { if (rs.wasNull() && isNullable) null else it }
//            else -> rs.getObject(columnName)
//        }
//
//    private fun getJdbcType(kClass: KClass<*>): Int =
//        when (kClass) {
//            String::class -> Types.VARCHAR
//            Int::class -> Types.INTEGER
//            Long::class -> Types.BIGINT
//            Double::class -> Types.DOUBLE
//            Boolean::class -> Types.BOOLEAN
//            LocalDateTime::class -> Types.TIMESTAMP
//            else -> Types.OTHER
//        }
// }
