package com.example.jobstat.community.board.service

import com.example.jobstat.community.board.repository.FakeCategoryRepository
import com.example.jobstat.core.error.AppException
import jakarta.persistence.EntityNotFoundException // Keep if Fake repo throws this for findById
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.function.Executable

@DisplayName("CategoryService 테스트 (Refactored)")
class CategoryServiceTest {
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var categoryService: CategoryService

    @BeforeEach
    fun setUp() {
        categoryRepository = FakeCategoryRepository()
        categoryService = CategoryServiceImpl(categoryRepository)
    }

    @AfterEach
    fun tearDown() {
        categoryRepository.clear()
    }

    @Nested
    @DisplayName("카테고리 생성 (createCategory)")
    inner class CreateCategory {
        @Test
        @DisplayName("성공: 유효한 정보로 카테고리를 생성한다")
        fun `given valid details, when createCategory, then return new category`() {
            // Given
            val name = "new-category"
            val displayName = "새 카테고리"
            val description = "설명입니다."

            // When
            val createdCategory = categoryService.createCategory(name, displayName, description)

            // Then
            assertNotNull(createdCategory)
            assertTrue(createdCategory.id > 0)
            assertEquals(name, createdCategory.name)
            assertEquals(displayName, createdCategory.displayName)
            assertEquals(description, createdCategory.description)

            // Verify
            val found = categoryRepository.findById(createdCategory.id)
            assertEquals(createdCategory.id, found.id)
        }

        @Test
        @DisplayName("실패: 이미 존재하는 이름으로 생성 시 AppException(DUPLICATE_RESOURCE) 발생")
        fun `given existing name, when createCategory, then throw DuplicateResource exception`() {
            // Given
            val existingName = "existing-category"
            categoryService.createCategory(existingName, "기존 카테고리", "설명")

            assertThrows<AppException> {
                categoryService.createCategory(existingName, "다른 표시 이름", "다른 설명")
            }
        }

        @Test
        @DisplayName("실패: 빈 이름으로 생성 시 IllegalArgumentException 발생 (Entity 레벨)")
        fun `given blank name, when createCategory, then throw IllegalArgumentException`() {
            // Given
            val emptyName = ""
            val blankName = "   "

            // When & Then
            assertThrows<IllegalArgumentException>("빈 이름") {
                categoryService.createCategory(emptyName, "Display", "Desc")
            }
            assertThrows<IllegalArgumentException>("공백 이름") {
                categoryService.createCategory(blankName, "Display", "Desc")
            }
        }

        @Test
        @DisplayName("성공: 삭제된 카테고리와 동일한 이름으로 새 카테고리를 생성할 수 있다")
        fun `given deleted category name, when createCategory, then success`() {
            // Given
            val name = "reusable-category"
            val initialCategory = categoryService.createCategory(name, "Reusable", "Desc")
            categoryService.deleteCategory(initialCategory.id) // 삭제

            // When
            val executable =
                Executable {
                    categoryService.createCategory(name, "New Reusable", "New Desc")
                }

            // Then
            assertDoesNotThrow(executable)
            val newCategory = categoryRepository.findByName(name)
            assertNotNull(newCategory)
            assertNotEquals(initialCategory.id, newCategory.id)
            assertEquals("New Reusable", newCategory.displayName)
        }
    }

    @Nested
    @DisplayName("카테고리 조회 (getCategoryById)")
    inner class GetCategoryById {
        @Test
        @DisplayName("성공: 존재하는 ID로 카테고리를 조회한다")
        fun `given existing id, when getCategoryById, then return category`() {
            // Given
            val savedCategory = categoryService.createCategory("test-cat", "Test Cat", "Desc")

            // When
            val foundCategory = categoryService.getCategoryById(savedCategory.id)

            // Then
            assertNotNull(foundCategory)
            assertEquals(savedCategory.id, foundCategory.id)
            assertEquals(savedCategory.name, foundCategory.name)
        }

        @Test
        @DisplayName("실패: 존재하지 않는 ID로 조회 시 EntityNotFoundException 발생")
        fun `given non-existent id, when getCategoryById, then throw EntityNotFoundException`() {
            // Given
            val nonExistentId = 999L

            assertThrows<EntityNotFoundException> {
                categoryService.getCategoryById(nonExistentId)
            }
        }
    }

    @Nested
    @DisplayName("모든 카테고리 조회 (getAllCategories)")
    inner class GetAllCategories {
        @Test
        @DisplayName("성공: 여러 카테고리가 있을 때 모두 조회한다")
        fun `given multiple categories, when getAllCategories, then return all`() {
            // Given
            val cat1 = categoryService.createCategory("cat1", "Cat 1", "D1")
            val cat2 = categoryService.createCategory("cat2", "Cat 2", "D2")
            val cat3 = categoryService.createCategory("cat3", "Cat 3", "D3")

            // When
            val categories = categoryService.getAllCategories()

            // Then
            assertEquals(3, categories.size)
            assertTrue(categories.any { it.id == cat1.id })
            assertTrue(categories.any { it.id == cat2.id })
            assertTrue(categories.any { it.id == cat3.id })
        }

        @Test
        @DisplayName("성공: 카테고리가 없을 때 빈 리스트를 반환한다")
        fun `given no categories, when getAllCategories, then return empty list`() {
            // When
            val categories = categoryService.getAllCategories()

            // Then
            assertTrue(categories.isEmpty())
        }

        @Test
        @DisplayName("성공: 삭제된 카테고리는 조회되지 않는다")
        fun `given deleted category, when getAllCategories, then it is not included`() {
            // Given
            val cat1 = categoryService.createCategory("cat1", "Cat 1", "D1")
            val catToDelete = categoryService.createCategory("cat-delete", "Delete Me", "Del")
            categoryService.deleteCategory(catToDelete.id)

            // When
            val categories = categoryService.getAllCategories()

            // Then
            assertEquals(1, categories.size)
            assertEquals(cat1.id, categories[0].id)
            assertFalse(categories.any { it.id == catToDelete.id })
        }
    }

    @Nested
    @DisplayName("카테고리 수정 (updateCategory)")
    inner class UpdateCategory {
        @Test
        @DisplayName("성공: 존재하는 카테고리의 정보를 수정한다")
        fun `given existing category and valid details, when updateCategory, then return updated category`() {
            // Given
            val originalCategory = categoryService.createCategory("original", "Original", "Orig Desc")
            val newName = "updated-name"
            val newDisplayName = "업데이트된 이름"
            val newDescription = "수정된 설명"

            // When
            val updatedCategory = categoryService.updateCategory(originalCategory.id, newName, newDisplayName, newDescription)

            // Then
            assertNotNull(updatedCategory)
            assertEquals(originalCategory.id, updatedCategory.id)
            assertEquals(newName, updatedCategory.name)
            assertEquals(newDisplayName, updatedCategory.displayName)
            assertEquals(newDescription, updatedCategory.description)

            // Verify persistence
            val found = categoryRepository.findById(originalCategory.id)
            assertEquals(newName, found.name)
            assertEquals(newDisplayName, found.displayName)
        }

        @Test
        @DisplayName("실패: 존재하지 않는 카테고리 수정 시 EntityNotFoundException 발생")
        fun `given non-existent id, when updateCategory, then throw EntityNotFoundException`() {
            // Given
            val nonExistentId = 999L

            // Then
            assertThrows<EntityNotFoundException> {
                categoryService.updateCategory(nonExistentId, "new-name", "New Name", "New Desc")
            }
        }

        @Test
        @DisplayName("실패: 다른 카테고리가 이미 사용중인 이름으로 수정 시 DuplicateKeyException 발생 (Fake Repo 레벨)")
        fun `given name used by another category, when updateCategory, then throw exception`() {
            // Given
            val existingCategory = categoryService.createCategory("existing-name", "Existing", "Desc")
            val targetCategory = categoryService.createCategory("target-name", "Target", "Desc")

            assertThrows<AppException> {
                categoryService.updateCategory(targetCategory.id, existingCategory.name, "New Display", "New Desc")
            }
        }

        @Test
        @DisplayName("실패: 빈 이름으로 수정 시 IllegalArgumentException 발생 (Entity 레벨)")
        fun `given blank name, when updateCategory, then throw IllegalArgumentException`() {
            // Given
            val savedCategory = categoryService.createCategory("original", "Original", "Desc")

            // When & Then
            assertThrows<IllegalArgumentException>("빈 이름") {
                categoryService.updateCategory(savedCategory.id, "", "New Display", "New Desc")
            }
            assertThrows<IllegalArgumentException>("공백 이름") {
                categoryService.updateCategory(savedCategory.id, "   ", "New Display", "New Desc")
            }
        }

        @Test
        @DisplayName("성공: 자기 자신의 이름으로 수정은 가능하다")
        fun `given same name, when updateCategory, then success`() {
            // Given
            val name = "same-name"
            val savedCategory = categoryService.createCategory(name, "Same Display", "Desc")

            // When
            val executable =
                Executable {
                    categoryService.updateCategory(savedCategory.id, name, "Updated Display", "Updated Desc")
                }

            // Then
            assertDoesNotThrow(executable)
            val updated = categoryService.getCategoryById(savedCategory.id)
            assertEquals(name, updated.name)
            assertEquals("Updated Display", updated.displayName) // Verify other fields updated
        }
    }

    @Nested
    @DisplayName("카테고리 삭제 (deleteCategory)")
    inner class DeleteCategory {
        @Test
        @DisplayName("성공: 존재하는 카테고리를 삭제한다")
        fun `given existing category, when deleteCategory, then category is deleted`() {
            // Given
            val categoryToDelete = categoryService.createCategory("to-delete", "Delete Me", "Desc")
            val categoryId = categoryToDelete.id

            // When
            categoryService.deleteCategory(categoryId)

            // Then
            assertThrows<EntityNotFoundException>("삭제 후 조회 시 실패해야 함") {
                categoryService.getCategoryById(categoryId)
            }
        }

        @Test
        @DisplayName("성공: 존재하지 않는 카테고리 삭제 시 예외가 발생하지 않는다 (deleteById 동작)")
        fun `given non-existent id, when deleteCategory, then no exception`() {
            // Given
            val nonExistentId = 999L

            // When
            val executable =
                Executable {
                    categoryService.deleteCategory(nonExistentId)
                }

            // Then
            assertDoesNotThrow(executable)
        }
    }

    @Nested
    @DisplayName("카테고리 이름 가용성 확인 (isCategoryNameAvailable)")
    inner class CategoryNameAvailability {
        @Test
        @DisplayName("성공: 사용 가능한 이름은 true를 반환한다")
        fun `given available name, when isCategoryNameAvailable, then return true`() {
            // Given
            categoryService.createCategory("existing-name", "Existing", "Desc")
            val availableName = "new-name"

            // When
            val isAvailable = categoryService.isCategoryNameAvailable(availableName)

            // Then
            assertTrue(isAvailable)
        }

        @Test
        @DisplayName("성공: 이미 존재하는 이름은 false를 반환한다")
        fun `given existing name, when isCategoryNameAvailable, then return false`() {
            // Given
            val existingName = "existing-name"
            categoryService.createCategory(existingName, "Existing", "Desc")

            // When
            val isAvailable = categoryService.isCategoryNameAvailable(existingName)

            // Then
            assertFalse(isAvailable)
        }

        @Test
        @DisplayName("성공: 삭제된 카테고리의 이름은 사용 가능하다 (true 반환)")
        fun `given deleted category name, when isCategoryNameAvailable, then return true`() {
            // Given
            val name = "reusable-name"
            val category = categoryService.createCategory(name, "Reusable", "Desc")
            categoryService.deleteCategory(category.id)

            // When
            val isAvailable = categoryService.isCategoryNameAvailable(name)

            // Then
            assertTrue(isAvailable)
        }

        @Test
        @DisplayName("성공: 이름 확인 시 대소문자를 구분한다 (DB collation 따라 다를 수 있으나, Fake는 보통 구분)")
        fun `given case difference, when isCategoryNameAvailable, then treat as different`() {
            // Given
            val nameLower = "testname"
            categoryService.createCategory(nameLower, "Test Name", "Desc")

            // When
            val availableUpper = categoryService.isCategoryNameAvailable("TESTNAME")
            val availableMixed = categoryService.isCategoryNameAvailable("TestName")
            val unavailableLower = categoryService.isCategoryNameAvailable(nameLower)

            // Then
            assertTrue(availableUpper, "Uppercase should be available")
            assertTrue(availableMixed, "Mixed case should be available")
            assertFalse(unavailableLower, "Original case should not be available")
        }
    }
}
