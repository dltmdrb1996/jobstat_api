// package com.example.jobstat.community.service
//
// import com.example.jobstat.community.board.entity.BoardCategory
// import com.example.jobstat.community.board.service.CategoryService
// import com.example.jobstat.community.board.service.CategoryServiceImpl
// import com.example.jobstat.community.fake.repository.FakeCategoryRepository
// import com.example.jobstat.core.error.AppException
// import com.example.jobstat.core.error.ErrorCode
// import jakarta.persistence.EntityNotFoundException
// import org.junit.jupiter.api.*
// import org.junit.jupiter.api.Assertions.*
// import kotlin.test.assertFailsWith
//
// @DisplayName("CategoryService 테스트")
// class CategoryServiceTest {
//    private lateinit var categoryRepository: FakeCategoryRepository
//    private lateinit var categoryService: CategoryService
//
//    @BeforeEach
//    fun setUp() {
//        categoryRepository = FakeCategoryRepository()
//        categoryService = CategoryServiceImpl(categoryRepository)
//    }
//
//    @Nested
//    @DisplayName("카테고리 생성")
//    inner class CreateCategory {
//        @Test
//        @DisplayName("유효한 이름으로 카테고리를 생성할 수 있다")
//        fun createValidCategory() {
//            val createdCategory =
//                categoryService.createCategory(
//                    "카테고리1",
//                    "카테고리 1",
//                    "카테고리 설명 1",
//                )
//            assertEquals("카테고리1", createdCategory.name)
//            assertTrue((createdCategory as com.example.jobstat.community.board.entity.BoardCategory).id > 0)
//        }
//
//        @Test
//        @DisplayName("중복된 이름으로 카테고리를 생성할 수 없다")
//        fun cannotCreateDuplicateCategory() {
//            categoryService.createCategory("중복", "중복 카테고리", "설명")
//            val ex =
//                assertFailsWith<AppException> {
//                    categoryService.createCategory("중복", "중복 카테고리", "설명")
//                }
//            assertEquals(ErrorCode.DUPLICATE_RESOURCE, ex.errorCode)
//        }
//
//        @Test
//        @DisplayName("빈 이름으로 카테고리를 생성할 수 없다")
//        fun cannotCreateEmptyNameCategory() {
//            assertFailsWith<IllegalArgumentException> {
//                categoryService.createCategory("", "Display", "Desc")
//            }
//            assertFailsWith<IllegalArgumentException> {
//                categoryService.createCategory("   ", "Display", "Desc")
//            }
//        }
//
//        @Test
//        @DisplayName("삭제된 카테고리와 동일한 이름으로 새 카테고리를 생성할 수 있다")
//        fun canCreateWithDeletedCategoryName() {
//            val category = categoryService.createCategory("Reusable", "Reusable", "Desc")
//            categoryService.deleteCategory(category.id)
//            assertDoesNotThrow {
//                categoryService.createCategory("Reusable", "Reusable", "Desc")
//            }
//        }
//    }
//
//    @Nested
//    @DisplayName("카테고리 조회")
//    inner class GetCategory {
//        @Test
//        @DisplayName("ID로 카테고리를 조회할 수 있다")
//        fun getCategoryById() {
//            val savedCategory = categoryService.createCategory("Test", "Test", "Desc")
//            val foundCategory = categoryService.getCategoryById(savedCategory.id)
//            assertEquals(savedCategory.id, foundCategory.id)
//            assertEquals(savedCategory.name, foundCategory.name)
//        }
//
//        @Test
//        @DisplayName("존재하지 않는 ID로 조회시 예외가 발생한다")
//        fun cannotGetNonExistentCategory() {
//            assertFailsWith<EntityNotFoundException> {
//                categoryService.getCategoryById(999L)
//            }
//        }
//
//        @Test
//        @DisplayName("모든 카테고리를 조회할 수 있다")
//        fun getAllCategories() {
//            val category1 = categoryService.createCategory("Category 1", "Cat1", "Desc1")
//            val category2 = categoryService.createCategory("Category 2", "Cat2", "Desc2")
//            val category3 = categoryService.createCategory("Category 3", "Cat3", "Desc3")
//            val categories = categoryService.getAllCategories()
//            assertEquals(3, categories.size)
//            assertTrue(categories.any { it.id == category1.id })
//            assertTrue(categories.any { it.id == category2.id })
//            assertTrue(categories.any { it.id == category3.id })
//        }
//
//        @Test
//        @DisplayName("카테고리가 없으면 빈 리스트를 반환한다")
//        fun getAllCategoriesWhenEmpty() {
//            val categories = categoryService.getAllCategories()
//            assertTrue(categories.isEmpty())
//        }
//    }
//
//    @Nested
//    @DisplayName("카테고리 수정")
//    inner class UpdateCategory {
//        @Test
//        @DisplayName("카테고리 이름을 수정할 수 있다")
//        fun updateCategoryName() {
//            val savedCategory = categoryService.createCategory("Old Name", "Old", "Desc")
//            val updatedCategory = categoryService.updateCategory(savedCategory.id, "New Name", "New", "New Desc")
//            assertEquals("New Name", updatedCategory.name)
//        }
//
//        @Test
//        @DisplayName("존재하지 않는 카테고리는 수정할 수 없다")
//        fun cannotUpdateNonExistentCategory() {
//            assertFailsWith<EntityNotFoundException> {
//                categoryService.updateCategory(999L, "New Name", "New", "New Desc")
//            }
//        }
//
//        @Test
//        @DisplayName("이미 존재하는 이름으로 수정할 수 없다")
//        fun cannotUpdateToDuplicateName() {
//            categoryService.createCategory("Existing", "Existing", "Desc")
//            val targetCategory = categoryService.createCategory("Target", "Target", "Desc")
//            val ex =
//                assertFailsWith<AppException> {
//                    categoryService.updateCategory(targetCategory.id, "Existing", "New", "Desc")
//                }
//            assertEquals(ErrorCode.DUPLICATE_RESOURCE, ex.errorCode)
//        }
//
//        @Test
//        @DisplayName("빈 이름으로 수정할 수 없다")
//        fun cannotUpdateToEmptyName() {
//            val savedCategory = categoryService.createCategory("Original", "Original", "Desc")
//            assertFailsWith<IllegalArgumentException> {
//                categoryService.updateCategory(savedCategory.id, "", "New", "New Desc")
//            }
//            assertFailsWith<IllegalArgumentException> {
//                categoryService.updateCategory(savedCategory.id, "   ", "New", "New Desc")
//            }
//        }
//
//        @Test
//        @DisplayName("같은 이름으로 수정할 수 있다")
//        fun canUpdateToSameName() {
//            val savedCategory = categoryService.createCategory("Same Name", "Same", "Desc")
//            assertDoesNotThrow {
//                categoryService.updateCategory(savedCategory.id, "Same Name", "Same", "Desc")
//            }
//        }
//    }
//
//    @Nested
//    @DisplayName("카테고리 삭제")
//    inner class DeleteCategory {
//        @Test
//        @DisplayName("카테고리를 삭제할 수 있다")
//        fun deleteCategory() {
//            val savedCategory = categoryService.createCategory("To be deleted", "Delete", "Desc")
//            categoryService.deleteCategory(savedCategory.id)
//            assertFailsWith<EntityNotFoundException> {
//                categoryService.getCategoryById(savedCategory.id)
//            }
//        }
//
//        @Test
//        @DisplayName("존재하지 않는 카테고리를 삭제해도 예외가 발생하지 않는다")
//        fun deleteNonExistentCategory() {
//            assertDoesNotThrow {
//                categoryService.deleteCategory(999L)
//            }
//        }
//
//        @Test
//        @DisplayName("삭제된 카테고리는 조회되지 않는다")
//        fun deletedCategoryNotFound() {
//            val category = categoryService.createCategory("To be deleted", "Delete", "Desc")
//            val allBeforeDelete = categoryService.getAllCategories()
//            assertEquals(1, allBeforeDelete.size)
//            categoryService.deleteCategory(category.id)
//            val allAfterDelete = categoryService.getAllCategories()
//            assertTrue(allAfterDelete.isEmpty())
//        }
//    }
//
//    @Nested
//    @DisplayName("카테고리 이름 가용성")
//    inner class CategoryNameAvailability {
//        @Test
//        @DisplayName("사용 가능한 이름인지 확인할 수 있다")
//        fun checkNameAvailability() {
//            categoryService.createCategory("Existing", "Existing", "Desc")
//            assertTrue(categoryService.isCategoryNameAvailable("New Category"))
//            assertFalse(categoryService.isCategoryNameAvailable("Existing"))
//        }
//
//        @Test
//        @DisplayName("삭제된 카테고리의 이름은 사용 가능하다")
//        fun deletedCategoryNameIsAvailable() {
//            val category = categoryService.createCategory("Reusable", "Reusable", "Desc")
//            assertFalse(categoryService.isCategoryNameAvailable("Reusable"))
//            categoryService.deleteCategory(category.id)
//            assertTrue(categoryService.isCategoryNameAvailable("Reusable"))
//        }
//
//        @Test
//        @DisplayName("이름 검사시 대소문자를 구분한다")
//        fun nameCheckIsCaseSensitive() {
//            categoryService.createCategory("Test", "Test", "Desc")
//            assertFalse(categoryService.isCategoryNameAvailable("Test"))
//            assertTrue(categoryService.isCategoryNameAvailable("test"))
//            assertTrue(categoryService.isCategoryNameAvailable("TEST"))
//        }
//    }
// }
