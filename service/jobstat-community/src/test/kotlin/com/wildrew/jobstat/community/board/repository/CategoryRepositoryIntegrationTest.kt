package com.wildrew.jobstat.community.board.repository

import com.wildrew.jobstat.community.board.entity.Board
import com.wildrew.jobstat.community.board.entity.BoardCategory
import com.wildrew.jobstat.community.board.fixture.BoardFixture
import com.wildrew.jobstat.community.board.fixture.CategoryFixture
import com.wildrew.jobstat.community.comment.repository.CommentRepository
import com.wildrew.jobstat.community.utils.base.JpaIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.exception.ConstraintViolationException
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException

@DisplayName("CategoryRepository 통합 테스트")
class CategoryRepositoryIntegrationTest : JpaIntegrationTestSupport() {
    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var boardRepository: BoardRepository

    @Autowired
    private lateinit var commentRepository: CommentRepository

    override fun cleanupTestData() {
        try {
            entityManager.clear()

            executeInTransaction {
                val comments = commentRepository.findAll(PageRequest.of(0, Int.MAX_VALUE)).content
                comments.forEach { commentRepository.deleteById(it.id) }

                val boards = boardRepository.findAll(PageRequest.of(0, Int.MAX_VALUE)).content
                boards.forEach { boardRepository.deleteById(it.id) }

                val categories = categoryRepository.findAll()
                categories.forEach { categoryRepository.deleteById(it.id) }
            }
            flushAndClear()
        } catch (e: Exception) {
            System.err.println("!!! 에러 발생: cleanupTestData 중 예외 발생 !!! - ${e.message}")
        }
    }

    @BeforeEach
    fun setUp() {
        cleanupTestData()
    }

    @AfterEach
    override fun tearDown() {
        cleanupTestData()
    }

    private fun createAndSaveCategory(
        name: String = "DEFAULT_CAT",
        displayName: String = "Default",
    ): BoardCategory {
        val category =
            CategoryFixture
                .aCategory()
                .withName(name)
                .withDisplayName(displayName)
                .create()
        return saveAndFlush(category) { categoryRepository.save(it) }
    }

    private fun createAndSaveBoard(
        category: BoardCategory,
        title: String,
    ): Board {
        val board =
            BoardFixture
                .aBoard()
                .withCategory(category)
                .withTitle(title)
                .create()
        return saveAndFlush(board) { boardRepository.save(it) }
    }

    @Nested
    @DisplayName("카테고리 생성 테스트")
    inner class CreateCategoryTest {
        @Test
        @DisplayName("새로운 카테고리를 생성하고 저장할 수 있다")
        fun `save success - new category`() {
            // given
            val name = "NEW_CAT"
            val displayName = "New Category"
            val description = "Desc for new category"
            val categoryToSave =
                CategoryFixture
                    .aCategory()
                    .withName(name)
                    .withDisplayName(displayName)
                    .withDescription(description)
                    .create()

            // when
            val savedCategory = saveAndFlush(categoryToSave) { categoryRepository.save(it) }

            // then
            assertThat(savedCategory.id).isNotNull()
            assertThat(savedCategory.name).isEqualTo(name)
            assertThat(savedCategory.displayName).isEqualTo(displayName)
            assertThat(savedCategory.description).isEqualTo(description)
            assertThat(savedCategory.createdAt).isNotNull()
            assertThat(savedCategory.updatedAt).isNotNull()

            // DB에서 다시 조회하여 확인
            val foundCategory = categoryRepository.findById(savedCategory.id)
            assertThat(foundCategory).isNotNull
            assertThat(foundCategory.name).isEqualTo(name)
        }

        @Test
        @DisplayName("중복된 이름(name)으로 카테고리를 생성하면 DataIntegrityViolationException 예외가 발생한다")
        fun `save fail - duplicate name`() {
            // given
            val existingCategory = createAndSaveCategory(name = "DUPLICATE_NAME")
            val newCategoryWithSameName = CategoryFixture.aCategory().withName(existingCategory.name).create()

            // when & then
            assertThrows<DataIntegrityViolationException> {
                saveAndFlush(newCategoryWithSameName) { categoryRepository.save(it) }
            }
        }

        @Test
        @DisplayName("카테고리 생성 시 이름(name)이 비어있거나 공백이면 IllegalArgumentException 예외가 발생한다")
        fun `create fail - blank name`() {
            // given
            val displayName = "Test Display"
            val description = "Test Desc"

            // when & then
            assertThrows<IllegalArgumentException>("이름(name) 공백 예외") {
                BoardCategory.create("", displayName, description)
            }
            assertThrows<IllegalArgumentException>("이름(name) 공백 예외") {
                BoardCategory.create("   ", displayName, description)
            }
        }

        @Test
        @DisplayName("카테고리 생성 시 표시 이름(displayName)이 비어있거나 공백이면 IllegalArgumentException 예외가 발생한다")
        fun `create fail - blank displayName`() {
            // given
            val name = "VALID_NAME"
            val description = "Test Desc"

            // when & then
            assertThrows<IllegalArgumentException>("표시 이름(displayName) 공백 예외") {
                BoardCategory.create(name, "", description)
            }
            assertThrows<IllegalArgumentException>("표시 이름(displayName) 공백 예외") {
                BoardCategory.create(name, "   ", description)
            }
        }
    }

    @Nested
    @DisplayName("카테고리 조회 테스트")
    inner class ReadCategoryTest {
        private lateinit var cat1: BoardCategory
        private lateinit var cat2: BoardCategory

        @BeforeEach
        fun setupReadData() {
            cat1 = createAndSaveCategory("CAT1", "카테고리1")
            cat2 = createAndSaveCategory("CAT2", "카테고리2")
        }

        @Test
        @DisplayName("ID로 카테고리를 성공적으로 조회한다")
        fun `findById success`() {
            // given
            val targetId = cat1.id

            // when
            val foundCategory = categoryRepository.findById(targetId)

            // then
            assertThat(foundCategory).isNotNull
            assertThat(foundCategory.id).isEqualTo(targetId)
            assertThat(foundCategory.name).isEqualTo(cat1.name)
        }

        @Test
        @DisplayName("존재하지 않는 ID로 카테고리 조회 시 JpaObjectRetrievalFailureException 예외가 발생한다")
        fun `findById fail - not found`() {
            // given
            val nonExistentId = 9999L

            // when & then
            assertThrows<JpaObjectRetrievalFailureException> {
                categoryRepository.findById(nonExistentId)
            }
        }

        @Test
        @DisplayName("이름(name)으로 카테고리를 성공적으로 조회한다")
        fun `findByName success`() {
            // given
            val targetName = cat2.name

            // when
            val foundCategory = categoryRepository.findByName(targetName)

            // then
            assertThat(foundCategory).isNotNull
            assertThat(foundCategory.id).isEqualTo(cat2.id)
            assertThat(foundCategory.name).isEqualTo(targetName)
        }

        @Test
        @DisplayName("존재하지 않는 이름(name)으로 카테고리 조회 시 JpaObjectRetrievalFailureException 예외가 발생한다")
        fun `findByName fail - not found`() {
            // given
            val nonExistentName = "NON_EXISTENT_NAME"

            // when & then
            assertThrows<JpaObjectRetrievalFailureException> {
                categoryRepository.findByName(nonExistentName)
            }
        }

        @Test
        @DisplayName("모든 카테고리 목록을 성공적으로 조회한다")
        fun `findAll success`() {
            // when
            val allCategories = categoryRepository.findAll()

            // then
            assertThat(allCategories).hasSize(2)
            assertThat(allCategories).extracting("id").containsExactlyInAnyOrder(cat1.id, cat2.id)
        }

        @Test
        @DisplayName("카테고리가 없을 때 findAll 호출 시 빈 목록을 반환한다")
        fun `findAll success - empty result`() {
            // given: 데이터 없음 (setup 후 cleanup 가정)
            cleanupTestData()

            // when
            val allCategories = categoryRepository.findAll()

            // then
            assertThat(allCategories).isEmpty()
        }

        @Test
        @DisplayName("각 카테고리별 게시글 수를 포함하여 모든 카테고리 목록을 조회한다")
        fun `findAllWithBoardCount success`() {
            // given: cat1, cat2 생성됨
            createAndSaveBoard(cat1, "Board 1 in Cat1")
            createAndSaveBoard(cat1, "Board 2 in Cat1")
            createAndSaveBoard(cat2, "Board 1 in Cat2")
            flushAndClear()

            // when
            val results = categoryRepository.findAllWithBoardCount()

            // then
            assertThat(results).hasSize(2)

            val cat1Result = results.find { it.first.id == cat1.id }
            val cat2Result = results.find { it.first.id == cat2.id }

            assertThat(cat1Result).isNotNull
            assertThat(cat1Result!!.first.name).isEqualTo(cat1.name)
            assertThat(cat1Result.second).isEqualTo(2) // cat1의 게시글 수

            assertThat(cat2Result).isNotNull
            assertThat(cat2Result!!.first.name).isEqualTo(cat2.name)
            assertThat(cat2Result.second).isEqualTo(1) // cat2의 게시글 수
        }

        @Test
        @DisplayName("게시글이 없는 카테고리의 게시글 수는 0으로 조회된다")
        fun `findAllWithBoardCount success - zero count`() {
            // given: cat1, cat2 생성됨 (게시글 없음)
            flushAndClear()

            // when
            val results = categoryRepository.findAllWithBoardCount()

            // then
            assertThat(results).hasSize(2)
            results.forEach { (_, count) ->
                assertThat(count).isEqualTo(0)
            }
        }

        @Test
        @DisplayName("이름(name)으로 카테고리 존재 여부를 확인한다")
        fun `existsByName success`() {
            // given: setupReadData()에서 cat1 생성됨

            // when
            val exists = categoryRepository.existsByName(cat1.name)
            val notExists = categoryRepository.existsByName("NON_EXISTENT")

            // then
            assertThat(exists).isTrue()
            assertThat(notExists).isFalse()
        }
    }

    @Nested
    @DisplayName("카테고리 수정/삭제 테스트")
    inner class UpdateDeleteCategoryTest {
        private lateinit var categoryToModify: BoardCategory

        @BeforeEach
        fun setupModifyData() {
            categoryToModify = createAndSaveCategory("MODIFY_ME", "수정 전")
        }

        @Test
        @DisplayName("카테고리 정보를 성공적으로 수정한다 (엔티티 메소드 + save)")
        fun `update success - using entity method`() {
            // given
            val newName = "MODIFIED_NAME"
            val newDisplayName = "수정된 카테고리"
            val newDescription = "수정된 설명입니다."

            // when
            // 1. 영속성 컨텍스트에서 엔티티 조회
            val foundCategory = categoryRepository.findById(categoryToModify.id)
            // 2. 엔티티 메소드를 사용하여 상태 변경
            foundCategory.updateCategory(newName, newDisplayName, newDescription)
            // 3. save 호출 (변경 감지로 인해 생략 가능하나 명시적으로 호출) 및 flush
            saveAndFlush(foundCategory) { categoryRepository.save(it) }
            // 4. 영속성 컨텍스트 클리어 후 다시 조회하여 확인
            flushAndClear()
            val updatedCategory = categoryRepository.findById(categoryToModify.id)

            // then
            assertThat(updatedCategory.name).isEqualTo(newName)
            assertThat(updatedCategory.displayName).isEqualTo(newDisplayName)
            assertThat(updatedCategory.description).isEqualTo(newDescription)
        }

        @Test
        @DisplayName("카테고리 수정 시 이름(name)을 빈 값으로 변경하면 IllegalArgumentException 예외가 발생한다")
        fun `update fail - blank name`() {
            // given
            val foundCategory = categoryRepository.findById(categoryToModify.id)

            // when & then
            assertThrows<IllegalArgumentException> {
                foundCategory.updateCategory("", "Valid Display", "Valid Desc")
            }
            assertThrows<IllegalArgumentException> {
                foundCategory.updateCategory("   ", "Valid Display", "Valid Desc")
            }
        }

        @Test
        @DisplayName("카테고리 수정 시 표시 이름(displayName)을 빈 값으로 변경하면 IllegalArgumentException 예외가 발생한다")
        fun `update fail - blank displayName`() {
            // given
            val foundCategory = categoryRepository.findById(categoryToModify.id)

            // when & then
            assertThrows<IllegalArgumentException> {
                foundCategory.updateCategory("ValidName", "", "Valid Desc")
            }
            assertThrows<IllegalArgumentException> {
                foundCategory.updateCategory("ValidName", "  ", "Valid Desc")
            }
        }

        @Test
        @DisplayName("카테고리 수정 시 이미 존재하는 다른 카테고리 이름(name)으로 변경하면 DataIntegrityViolationException 예외가 발생한다")
        fun `update fail - duplicate name`() {
            // given
            val existingCategory = createAndSaveCategory("EXISTING_NAME", "Existing")
            val categoryToUpdate = categoryToModify

            // when & then
            assertThrows<ConstraintViolationException> {
                categoryToUpdate.updateCategory(existingCategory.name, "New Display", "New Desc")
                saveAndFlush(categoryToUpdate) { categoryRepository.save(it) }
            }
        }

        @Test
        @DisplayName("게시글이 없는 카테고리를 성공적으로 삭제한다")
        fun `deleteById success - no boards`() {
            // given
            val categoryId = categoryToModify.id

            // when
            categoryRepository.deleteById(categoryId)
            flushAndClear()

            // then
            val exists = categoryRepository.existsByName(categoryToModify.name) // existsByName으로 확인
            assertThat(exists).isFalse()
            assertThrows<JpaObjectRetrievalFailureException>("삭제 후 findById 예외 확인") {
                categoryRepository.findById(categoryId)
            }
        }

        @Test
        @DisplayName("게시글이 있는 카테고리 삭제 시 연관된 게시글도 함께 삭제된다 (Cascade)")
        fun `deleteById success - with boards cascade`() {
            // given
            val categoryToDelete = categoryToModify
            val board1 = createAndSaveBoard(categoryToDelete, "Board 1")
            val board2 = createAndSaveBoard(categoryToDelete, "Board 2")
            flushAndClear()

            val categoryId = categoryToDelete.id
            val board1Id = board1.id
            val board2Id = board2.id

            // 게시글 존재 확인
            assertThat(boardRepository.existsById(board1Id)).isTrue()
            assertThat(boardRepository.existsById(board2Id)).isTrue()

            // when
            categoryRepository.deleteById(categoryId)
            flushAndClear()

            // then
            // 카테고리 삭제 확인
            assertThrows<JpaObjectRetrievalFailureException> {
                categoryRepository.findById(categoryId)
            }
            // 연관된 게시글도 삭제되었는지 확인 (CascadeType.ALL, orphanRemoval=true)
            assertThat(boardRepository.existsById(board1Id)).isFalse()
            assertThat(boardRepository.existsById(board2Id)).isFalse()
        }
    }
}
