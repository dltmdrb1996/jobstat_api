package com.wildrew.ksp.converter

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.getDeclaredProperties

class MongoEntityProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("org.springframework.data.mongodb.core.mapping.Document")
            .filterIsInstance<KSClassDeclaration>()
            .filterNot { it.isAbstract() }
            .toList()

        symbols.forEach { classDeclaration ->
            try {
                processEntity(classDeclaration, resolver)
            } catch (e: Exception) {
                logger.error("Failed to process ${classDeclaration.simpleName.asString()}: ${e.message}")
                e.printStackTrace()
            }
        }

        return emptyList()
    }

    private fun processEntity(classDeclaration: KSClassDeclaration, resolver: Resolver) {
        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()
        val entityInfoClassName = "${className}EntityInformation"

        // Get collection name from @Document annotation
        val collectionName = classDeclaration.annotations
            .find { it.annotationType.resolve().declaration.qualifiedName?.asString() == "org.springframework.data.mongodb.core.mapping.Document" }
            ?.arguments
            ?.find { it.name?.asString() == "collection" }
            ?.value as? String
            ?: className.lowercase()

        // Get all properties including those from superclasses
        val allProperties = getAllProperties(classDeclaration)

        generateEntityInformation(
            classDeclaration = classDeclaration,
            packageName = packageName,
            className = className,
            entityInfoClassName = entityInfoClassName,
            collectionName = collectionName,
            properties = allProperties
        )
    }

    private fun getAllProperties(classDeclaration: KSClassDeclaration): List<KSPropertyDeclaration> {
        val properties = mutableListOf<KSPropertyDeclaration>()
        var currentClass: KSClassDeclaration? = classDeclaration

        while (currentClass != null) {
            properties.addAll(
                currentClass.getDeclaredProperties().filter {
                    it.hasBackingField && !it.modifiers.contains(Modifier.PRIVATE)
                }
            )
            currentClass = currentClass.superTypes
                .firstOrNull()
                ?.resolve()
                ?.declaration as? KSClassDeclaration
        }

        return properties.distinctBy { it.simpleName.asString() }
    }

    private fun generateEntityInformation(
        classDeclaration: KSClassDeclaration,
        packageName: String,
        className: String,
        entityInfoClassName: String,
        collectionName: String,
        properties: List<KSPropertyDeclaration>
    ) {
        val dependencies = Dependencies(false, classDeclaration.containingFile!!)

        val file = codeGenerator.createNewFile(
            dependencies = dependencies,
            packageName = packageName,
            fileName = entityInfoClassName
        )

        val isVersioned = properties.any {
            it.annotations.any { ann ->
                ann.annotationType.resolve().declaration.qualifiedName?.asString() == "org.springframework.data.annotation.Version"
            }
        }

        file.write(
            """
            package $packageName
            
            import org.springframework.data.mongodb.repository.query.MongoEntityInformation
            import org.springframework.data.mongodb.core.query.Collation
            import org.springframework.stereotype.Component
            
            @Component
            class $entityInfoClassName : MongoEntityInformation<$className, String> {
                override fun getCollectionName(): String = "$collectionName"
                
                override fun getJavaType(): Class<$className> = $className::class.java
                
                override fun getIdType(): Class<String> = String::class.java
                
                override fun getId(entity: $className): String? = entity.id
                
                override fun getIdAttribute(): String = "id"
                
                override fun isNew(entity: $className): Boolean = entity.id == null
                
                override fun isVersioned(): Boolean = ${isVersioned}
                
                override fun getVersion(entity: $className): Any? = null
                
                override fun hasCollation(): Boolean = false
                
                override fun getCollation(): Collation? = null
                
                companion object {
                    private val INSTANCE = $entityInfoClassName()
                    
                    @JvmStatic
                    fun getInstance(): $entityInfoClassName = INSTANCE
                    
                    private val FIELD_MAPPINGS = mapOf(
                        ${properties.joinToString(",\n                            ") {
                "\"${it.simpleName.asString()}\" to \"${getMongoFieldName(it)}\""
            }}
                    )
                    
                    fun getMongoFieldName(fieldName: String): String = 
                        FIELD_MAPPINGS[fieldName] ?: fieldName
                }
            }
            
           
            """.trimIndent().toByteArray()
        )
    }

    private fun getMongoFieldName(property: KSPropertyDeclaration): String {
        // @Field annotation에서 name 값을 가져옴
        val fieldName = property.annotations
            .find { it.annotationType.resolve().declaration.qualifiedName?.asString() == "org.springframework.data.mongodb.core.mapping.Field" }
            ?.arguments
            ?.find { it.name?.asString() == "name" }
            ?.value as? String

        return when {
            fieldName?.isNotBlank() == true -> fieldName
            property.simpleName.asString() == "id" -> "_id"
            else -> property.simpleName.asString().replace(
                Regex("([a-z])([A-Z])"),
                "$1_$2"
            ).lowercase()
        }
    }
}

class MongoEntityProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment
    ): SymbolProcessor = MongoEntityProcessor(
        codeGenerator = environment.codeGenerator,
        logger = environment.logger
    )
}