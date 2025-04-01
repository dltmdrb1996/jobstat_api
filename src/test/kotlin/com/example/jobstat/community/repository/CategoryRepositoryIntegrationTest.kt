package com.example.jobstat.community.repository

import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.entity.BoardCategory
import com.example.jobstat.community.board.repository.BoardRepository
import com.example.jobstat.community.board.repository.CategoryRepository
import com.example.jobstat.utils.base.JpaIntegrationTestSupport
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException
import kotlin.test.*

@DisplayName("CategoryRepository 통합 테스트")
class CategoryRepositoryIntegrationTest : JpaIntegrationTestSupport() {
    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var boardRepository: BoardRepository

    private lateinit var testCategory: com.example.jobstat.community.board.entity.BoardCategory

    @BeforeEach
    fun setUp() {
        cleanupTestData()
        testCategory = com.example.jobstat.community.board.entity.BoardCategory.create("TEST_CATEGORY", "Test Category", "Test Description")
    }

    override fun cleanupTestData() {
        executeInTransaction {
            boardRepository.findAll(PageRequest.of(0, 100)).forEach { board ->
                boardRepository.delete(board)
            }
            categoryRepository.findAll().forEach { category ->
                categoryRepository.deleteById(category.id)
            }
        }
        flushAndClear()
    }

    @Nested
    @DisplayName("카테고리 생성 테스트")
    inner class CreateCategoryTest {
        @Test
        @DisplayName("새로운 카테고리를 생성할 수 있다")
        fun createCategorySuccess() {
            // When
            val savedCategory = categoryRepository.save(testCategory)

            // Then
            assertEquals(testCategory.name, savedCategory.name)
            assertTrue(savedCategory.id > 0)
        }

        @Test
        @DisplayName("중복된 이름으로 카테고리를 생성할 수 없다")
        fun createDuplicateNameFail() {
            // Given
            categoryRepository.save(testCategory)
            flushAndClear()

            // When & Then
            val duplicateCategory = com.example.jobstat.community.board.entity.BoardCategory.create(testCategory.name, testCategory.displayName, testCategory.description)
            assertFailsWith<DataIntegrityViolationException> {
                categoryRepository.save(duplicateCategory)
                flushAndClear()
            }
        }

        @Test
        @DisplayName("빈 이름으로 카테고리를 생성할 수 없다")
        fun createEmptyNameFail() {
            assertFailsWith<IllegalArgumentException> {
                com.example.jobstat.community.board.entity.BoardCategory.create("", "Empty", "Description")
            }
            assertFailsWith<IllegalArgumentException> {
                com.example.jobstat.community.board.entity.BoardCategory.create("   ", "Empty", "Description")
            }
        }
    }

    @Nested
    @DisplayName("카테고리 조회 테스트")
    inner class FindCategoryTest {
        @Test
        @DisplayName("ID로 카테고리를 조회할 수 있다")
        fun findByIdSuccess() {
            // Given
            val savedCategory = saveAndGetAfterCommit(testCategory) { categoryRepository.save(it) }

            // When
            val foundCategory = categoryRepository.findById(savedCategory.id)

            // Then
            assertEquals(savedCategory.id, foundCategory.id)
            assertEquals(savedCategory.name, foundCategory.name)
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회시 예외가 발생한다")
        fun findByNonExistentIdFail() {
            assertFailsWith<JpaObjectRetrievalFailureException> {
                categoryRepository.findById(999L)
            }
        }

        @Test
        @DisplayName("이름으로 카테고리를 조회할 수 있다")
        fun findByNameSuccess() {
            // Given
            val savedCategory = saveAndGetAfterCommit(testCategory) { categoryRepository.save(it) }

            // When
            val foundCategory = categoryRepository.findByName(savedCategory.name)

            // Then
            assertEquals(savedCategory.id, foundCategory.id)
            assertEquals(savedCategory.name, foundCategory.name)
        }

        @Test
        @DisplayName("존재하지 않는 이름으로 조회시 예외가 발생한다")
        fun findByNonExistentNameFail() {
            assertFailsWith<JpaObjectRetrievalFailureException> {
                categoryRepository.findByName("NON_EXISTENT")
            }
        }

        @Test
        @DisplayName("모든 카테고리를 조회할 수 있다")
        fun findAllSuccess() {
            // Given
            val category1 = saveAndGetAfterCommit(testCategory) { categoryRepository.save(it) }
            val category2 = saveAndGetAfterCommit(com.example.jobstat.community.board.entity.BoardCategory.create("CATEGORY_2", "Category 2", "Description 2")) { categoryRepository.save(it) }

            // When
            val categories = categoryRepository.findAll()

            // Then
            assertEquals(2, categories.size)
            assertTrue(categories.any { it.id == category1.id })
            assertTrue(categories.any { it.id == category2.id })
        }

        @Test
        @DisplayName("게시글 수와 함께 모든 카테고리를 조회할 수 있다")
        fun findAllWithBoardCountSuccess() {
            // Given
            val savedCategory = saveAndGetAfterCommit(testCategory) { categoryRepository.save(it) }

            // 첫 번째 카테고리에 게시글 2개 추가
            val board1 =
                Board.create(
                    title = "Title 1",
                    content = "Content 1",
                    author = "testUser",
                    password = null,
                    category = savedCategory,
                )
            val board2 =
                Board.create(
                    title = "Title 2",
                    content = "Content 2",
                    author = "testUser",
                    password = null,
                    category = savedCategory,
                )
            saveAndGetAfterCommit(board1) { boardRepository.save(it) }
            saveAndGetAfterCommit(board2) { boardRepository.save(it) }

            // 두 번째 카테고리에 게시글 1개 추가
            val category2 = saveAndGetAfterCommit(com.example.jobstat.community.board.entity.BoardCategory.create("CATEGORY_2", "Category 2", "Description 2")) { categoryRepository.save(it) }
            val board3 =
                Board.create(
                    title = "Title 3",
                    content = "Content 3",
                    author = "testUser",
                    password = null,
                    category = category2,
                )
            saveAndGetAfterCommit(board3) { boardRepository.save(it) }

            // When
            val categoriesWithCount = categoryRepository.findAllWithBoardCount()

            // Then
            assertEquals(2, categoriesWithCount.size)
            val category1Count = categoriesWithCount.find { it.first.id == savedCategory.id }?.second
            val category2Count = categoriesWithCount.find { it.first.id == category2.id }?.second
            assertEquals(2, category1Count)
            assertEquals(1, category2Count)
        }
    }

    @Nested
    @DisplayName("카테고리 수정/삭제 테스트")
    inner class UpdateDeleteCategoryTest {
        @Test
        @DisplayName("카테고리 이름을 수정할 수 있다")
        fun updateNameSuccess() {
            // Given
            val savedCategory = saveAndGetAfterCommit(testCategory) { categoryRepository.save(it) }

            // When
            savedCategory.updateName("NEW_NAME")
            val updatedCategory = saveAndGetAfterCommit(savedCategory) { categoryRepository.save(it) }

            // Then
            assertEquals("NEW_NAME", updatedCategory.name)
        }

        @Test
        @DisplayName("카테고리 이름을 빈 값으로 수정할 수 없다")
        fun updateEmptyNameFail() {
            // Given
            val savedCategory = saveAndGetAfterCommit(testCategory) { categoryRepository.save(it) }

            // When & Then
            assertFailsWith<IllegalArgumentException> {
                savedCategory.updateName("")
            }
            assertFailsWith<IllegalArgumentException> {
                savedCategory.updateName("   ")
            }
        }

        @Test
        @DisplayName("카테고리를 삭제할 수 있다")
        fun deleteCategorySuccess() {
            // Given
            val savedCategory = saveAndGetAfterCommit(testCategory) { categoryRepository.save(it) }

            // When
            categoryRepository.deleteById(savedCategory.id)
            flushAndClear()

            // Then
            assertFailsWith<JpaObjectRetrievalFailureException> {
                categoryRepository.findById(savedCategory.id)
            }
        }

        @Test
        @DisplayName("게시글이 있는 카테고리도 삭제할 수 있다")
        fun deleteCategoryWithBoardsSuccess() {
            // Given
            val savedCategory = saveAndGetAfterCommit(testCategory) { categoryRepository.save(it) }

            val board =
                Board.create(
                    title = "Test Title",
                    content = "Test Content",
                    author = "testUser",
                    password = null,
                    category = savedCategory,
                )
            saveAndGetAfterCommit(board) { boardRepository.save(it) }

            // When
            categoryRepository.deleteById(savedCategory.id)
            flushAndClear()

            // Then
            assertFailsWith<JpaObjectRetrievalFailureException> {
                categoryRepository.findById(savedCategory.id)
            }
            // 연관된 게시글도 삭제되었는지 확인
            assertTrue(boardRepository.findAll(PageRequest.of(0, 10)).isEmpty)
        }
    }
}
