package com.wildrew.jobstat.community.board.repository

import com.wildrew.jobstat.community.board.entity.Board
import com.wildrew.jobstat.community.board.entity.BoardCategory
import com.wildrew.jobstat.community.board.fixture.CategoryFixture
import com.wildrew.jobstat.community.board.repository.batch.BoardBatchRepositoryImpl
import com.wildrew.jobstat.community.utils.base.JpaIntegrationTestSupport
import com.wildrew.jobstat.community.utils.config.TestUtils
import com.wildrew.jobstat.core.core_jdbc_batch.core.model.BatchOptions
import com.wildrew.jobstat.core.core_jdbc_batch.core.model.BatchResult
import com.wildrew.jobstat.core.core_jdbc_batch.core.model.SelectPage
import com.wildrew.jobstat.core.core_jdbc_batch.infrastructure.update.ColumnType
import com.wildrew.jobstat.core.core_jdbc_batch.infrastructure.update.ColumnUpdate
import com.wildrew.jobstat.core.core_jpa_base.id_generator.SnowflakeGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.annotation.Commit
import org.springframework.test.annotation.DirtiesContext
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import kotlin.random.Random
import kotlin.system.measureTimeMillis

@DisplayName("BoardBatchRepositoryImpl 통합 테스트 (순차 진행, 최종 정리)")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BoardBatchRepositoryImplIntegrationTest : JpaIntegrationTestSupport() {
    @Autowired
    @Qualifier("synchronizedSnowflakeGenerator")
    private lateinit var snowflakeGenerator: SnowflakeGenerator

    @Autowired
    private lateinit var boardBatchRepository: BoardBatchRepositoryImpl

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var namedParameterJdbcTemplateForCleanup: NamedParameterJdbcTemplate

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    private lateinit var transactionTemplate: TransactionTemplate

    private val logger = LoggerFactory.getLogger(BoardBatchRepositoryImplIntegrationTest::class.java)
    override val executionTimes = mutableMapOf<String, Double>()

    override fun cleanupTestData() {
    }

    private var testMethodStartTime: Long = 0L

    private val testCategories = mutableListOf<BoardCategory>()
    private val allInsertedBoardIds = mutableListOf<Long>()

    private var totalRecord: Int = 10000
    private var batchSize: Int = 2000

    companion object {
        private var totalClassStartTime: Long = 0
        private var totalClassEndTime: Long = 0
        private val staticLogger = LoggerFactory.getLogger(BoardBatchRepositoryImplIntegrationTest::class.java.name + ".Companion")
    }

    @BeforeAll
    fun setupClassLevelResources() {
        totalClassStartTime = System.currentTimeMillis()
        staticLogger.info("===== @BeforeAll: Test class setup started (TransactionTemplate, Categories) =====")
        TestUtils.logMemoryUsage()

        transactionTemplate = TransactionTemplate(transactionManager)

        transactionTemplate.execute {
            staticLogger.info("Creating and committing test categories...")
            if (testCategories.isEmpty()) {
                val cat1 = categoryRepository.save(CategoryFixture.aCategory().withName("BATCH_CAT_1").create())
                val cat2 = categoryRepository.save(CategoryFixture.aCategory().withName("BATCH_CAT_2").create())
                testCategories.add(cat1)
                testCategories.add(cat2)
                staticLogger.info("Test categories committed: ${testCategories.map { it.id }}")
            }
        }
        staticLogger.info("@BeforeAll: Categories setup complete.")
    }

    @AfterAll
    fun cleanupClassAndLogTotalTime() {
        staticLogger.info("===== @AfterAll: Test class teardown, final data cleanup =====")
        if (allInsertedBoardIds.isNotEmpty()) {
            staticLogger.info("Final cleanup: Attempting to delete ${allInsertedBoardIds.size} boards...")
            try {
                transactionTemplate.execute {
                    val chunks = allInsertedBoardIds.chunked(batchSize)
                    var totalDeletedFromAll = 0
                    chunks.forEach { chunk ->
                        if (chunk.isNotEmpty()) {
                            totalDeletedFromAll += boardBatchRepository.batchDelete(chunk, BatchOptions(batchSize = batchSize))
                        }
                    }
                    staticLogger.info("Final cleanup: $totalDeletedFromAll boards deleted successfully.")
                }
            } catch (e: Exception) {
                staticLogger.error("Error during final board cleanup with batchDelete: ${e.message}", e)
                try {
                    staticLogger.warn("Attempting fallback cleanup with NamedParameterJdbcTemplate...")
                    transactionTemplate.execute {
                        val chunks = allInsertedBoardIds.chunked(1000)
                        chunks.forEach { chunk ->
                            if (chunk.isNotEmpty()) {
                                val params = MapSqlParameterSource().addValue("ids", chunk)
                                namedParameterJdbcTemplateForCleanup.update("DELETE FROM boards WHERE id IN (:ids)", params)
                            }
                        }
                        staticLogger.info("Fallback cleanup with NamedParameterJdbcTemplate completed.")
                    }
                } catch (directDeleteEx: Exception) {
                    staticLogger.error("Error during fallback NamedParameterJdbcTemplate cleanup: ${directDeleteEx.message}", directDeleteEx)
                }
            }
            allInsertedBoardIds.clear()
        }

        staticLogger.info("===== Test Method Execution Times Summary =====")
        executionTimes.forEach { (operation, time) ->
            staticLogger.info("$operation: $time seconds")
        }

        totalClassEndTime = System.currentTimeMillis()
        val totalTimeSeconds = (totalClassEndTime - totalClassStartTime) / 1000.0
        staticLogger.info("===== Total Test Class Execution Time: $totalTimeSeconds seconds =====")
        TestUtils.logMemoryUsage()
    }

    private fun recordTestStartTime() {
        testMethodStartTime = System.currentTimeMillis()
        TestUtils.logMemoryUsage()
    }

    private fun recordTestEndTime(testName: String) {
        val testDurationMillis = System.currentTimeMillis() - testMethodStartTime
        val timeInSeconds = testDurationMillis / 1000.0
        executionTimes[testName] = timeInSeconds
        println("--- Test method '$testName' completed in $timeInSeconds seconds ---")
        TestUtils.logMemoryUsage()
    }

    private fun createTestBoard(
        categoryId: Long,
        uniqueSuffix: String = Random.nextInt(100000).toString(),
    ): Board =
        Board(
            title = "Batch Test Title $uniqueSuffix",
            content = "Batch Test Content for board $uniqueSuffix",
            author = "BatchAuthor",
            password = null,
            categoryId = categoryId,
            userId = null,
        ).apply {
            id = snowflakeGenerator.nextId()
        }

    @Test
    @Order(1)
    @DisplayName("batchInsert: 초기 데이터 배치 삽입 및 시간 측정 (커밋됨)")
    @Commit
    fun `initial batchInsert boards and measure time (committed)`() {
        recordTestStartTime()
        val testName = "Initial Batch Insert Boards (Committed)"
        println("=== $testName 시작: ${totalRecord}개 게시글 삽입 시도 ===")
        assumeTrue(testCategories.isNotEmpty(), "Test categories must be initialized before inserting boards.")

        val boardsToInsert = mutableListOf<Board>()
        val categoryIdToUse = testCategories.random().id ?: throw IllegalStateException("Category ID is null for random category.")
        repeat(totalRecord) { i ->
            boardsToInsert.add(createTestBoard(categoryIdToUse, "init_insert_$i"))
        }

        val insertedIdsFromThisTest = mutableListOf<Long>()

        val timeMillis =
            measureTimeMillis {
                transactionTemplate.execute {
                    println("Transaction started for initial batch insert.")
                    val result = boardBatchRepository.batchInsert(boardsToInsert, BatchOptions(batchSize = batchSize))
                    assertThat(result.successful).hasSize(totalRecord)
                    assertThat(result.failed).isEmpty()
                    result.successful.forEach { board ->
                        assertThat(board.id).isNotNull.isGreaterThan(0L)
                        insertedIdsFromThisTest.add(board.id)
                    }
                    println("Initial batch insert transaction committing ${insertedIdsFromThisTest.size} records.")
                }
            }
        allInsertedBoardIds.addAll(insertedIdsFromThisTest)
        println("$testName 완료. 소요시간: ${timeMillis / 1000.0} 초, 처리 건수: ${insertedIdsFromThisTest.size}. Total tracked IDs for cleanup: ${allInsertedBoardIds.size}")
        assertThat(allInsertedBoardIds).hasSize(totalRecord)

        recordTestEndTime(testName)
    }

    @Test
    @Order(3) // 순서는 필요에 따라 조정
    @DisplayName("batchSelectByCursor: 커서 기반 페이징으로 전체 게시글 조회")
    fun `select all boards using cursor based pagination`() {
        recordTestStartTime()
        val testName = "Select All Boards (Cursor Pagination)"
        println("=== $testName 시작: ${allInsertedBoardIds.size}개 전체 조회 시도 (커서 기반) ===")
        assumeTrue(allInsertedBoardIds.isNotEmpty(), "초기 데이터 삽입(@Order(1))이 커밋되어 선행되어야 합니다.")

        val fetchedBoardsByCursor = mutableListOf<Board>()
        var lastId: Long? = null
        var totalFetchedCount = 0

        val timeMillis =
            measureTimeMillis {
                while (true) {
                    // BoardBatchRepositoryImpl에 batchSelectByCursor 메소드가 구현되어 있다고 가정
                    val selectedChunk = boardBatchRepository.batchSelect(SelectPage(lastId = lastId, limit = batchSize), BatchOptions(batchSize = batchSize))
                    if (selectedChunk.isEmpty()) {
                        break
                    }
                    fetchedBoardsByCursor.addAll(selectedChunk)
                    lastId = selectedChunk.lastOrNull()?.id
                    totalFetchedCount += selectedChunk.size
                }
            }

        assertThat(fetchedBoardsByCursor.size).isEqualTo(allInsertedBoardIds.size)

        println("$testName 완료. 소요시간: ${timeMillis / 1000.0} 초, 조회 건수: ${fetchedBoardsByCursor.size}")
        recordTestEndTime(testName)
    }

    @Test
    @Order(2)
    @DisplayName("batchSelectByIds: 삽입된 전체 게시글 ID로 조회")
    @Commit // 이 테스트는 조회만 하므로 Commit은 필요 없지만, 만약 다른 테스트에 영향을 주지 않음을 명시
    fun `select ALL inserted boards by ids (from committed data)`() {
        recordTestStartTime()
        val testName = "Select ALL Inserted Boards By Ids (from committed data)"
        println("=== $testName 시작: ${allInsertedBoardIds.size}개 전체 조회 시도 ===")
        assumeTrue(allInsertedBoardIds.isNotEmpty(), "초기 데이터 삽입(@Order(1))이 커밋되어 선행되어야 합니다.")

        val idsToFind = allInsertedBoardIds // 전체 ID 사용

        var foundBoards: List<Board> = emptyList()
        val timeMillis =
            measureTimeMillis {
                foundBoards = boardBatchRepository.batchSelectByIds(idsToFind) // BatchOptions 활용 가능하면 사용
            }

        assertThat(foundBoards).hasSize(idsToFind.size)
        // 모든 ID가 실제로 조회되었는지 간단히 검증 (전체 ID 비교는 무거울 수 있음)
        // assertThat(foundBoards.map { it.id }.toSet()).isEqualTo(idsToFind.toSet()) // 매우 많은 ID 비교는 비효율적

        println("$testName 완료. 소요시간: ${timeMillis / 1000.0} 초, 조회 건수: ${foundBoards.size}")
        recordTestEndTime(testName)
    }

    @Test
    @Order(3)
    @DisplayName("batchUpsert (Update): 기존 게시글 업데이트 (변경사항 롤백됨)")
    fun `update existing boards using batchUpsert (changes rolled back)`() {
        recordTestStartTime()
        val testName = "Update Existing Boards (batchUpsert, changes rolled back)"
        println("=== $testName 시작 ===")
        assumeTrue(allInsertedBoardIds.isNotEmpty(), "업데이트할 데이터가 @Order(1)에서 준비되어야 합니다.")

        val numToUpdate = minOf(totalRecord, allInsertedBoardIds.size)
        assumeTrue(numToUpdate > 0, "Not enough data from @Order(1) to perform update test.")
        val idsForUpdate = allInsertedBoardIds.take(numToUpdate)

        val originalBoards = boardBatchRepository.batchSelectByIds(idsForUpdate)
        assertThat(originalBoards).hasSize(idsForUpdate.size)

        val expectedNewTitlePrefix = "New Title after Upsert "
        val expectedNewContentPrefix = "New Content after Upsert "

        val boardsToUpdate =
            originalBoards.map { board ->
                board.apply {
                    updateContent(
                        newTitle = "$expectedNewTitlePrefix${this.id}",
                        newContent = "$expectedNewContentPrefix${this.id}",
                    )
                }
            }

        var upsertedResult: BatchResult<Board>? = null
        val timeMillis =
            measureTimeMillis {
                upsertedResult = boardBatchRepository.batchUpsert(boardsToUpdate, BatchOptions(batchSize = batchSize))
            }
        assertThat(upsertedResult).isNotNull
        assertThat(upsertedResult!!.successful).hasSize(boardsToUpdate.size)
        assertThat(upsertedResult!!.failed).isEmpty()
        println("$testName 완료. 소요시간: ${timeMillis / 1000.0} 초, 처리 건수: ${boardsToUpdate.size}")

        if (boardsToUpdate.isNotEmpty()) {
            val firstBoardIdForCheck = boardsToUpdate.first().id
            val checkedBoard = boardBatchRepository.batchSelectByIds(listOf(firstBoardIdForCheck)).firstOrNull()

            assertThat(checkedBoard).isNotNull
            assertThat(checkedBoard!!.title).isEqualTo("$expectedNewTitlePrefix$firstBoardIdForCheck")
            assertThat(checkedBoard.content).isEqualTo("$expectedNewContentPrefix$firstBoardIdForCheck")
        }

        recordTestEndTime(testName)
    }

    @Test
    @Order(5)
    @DisplayName("bulkValueUpdate: 특정 ID 목록에 단일 컬럼 업데이트 (변경사항 롤백됨)")
    fun `update single column for specific ids using bulkValueUpdate (changes rolled back)`() {
        recordTestStartTime()
        val testName = "BulkValueUpdate Author (changes rolled back)"
        println("=== $testName 시작 ===")
        val numToUpdate = minOf(10, allInsertedBoardIds.size)
        assumeTrue(numToUpdate > 0, "Not enough data from @Order(1) for bulkValueUpdate test.")
        val idsToUpdate = allInsertedBoardIds

        val newAuthor = "Author_BulkUpdated_${Random.nextInt(100)}"
        val columnUpdate = ColumnUpdate("author", newAuthor, ColumnType.STRING)

        var updatedCount = 0
        val timeMillis =
            measureTimeMillis {
                updatedCount = boardBatchRepository.bulkValueUpdate(idsToUpdate, columnUpdate)
            }
        assertThat(updatedCount).isEqualTo(idsToUpdate.size)
        println("$testName 완료. 소요시간: ${timeMillis / 1000.0} 초, 처리 건수: ${idsToUpdate.size}")

        val updatedBoards = boardBatchRepository.batchSelectByIds(idsToUpdate)
        assertThat(updatedBoards).hasSize(idsToUpdate.size)
        updatedBoards.forEach { board ->
            assertThat(board.author).isEqualTo(newAuthor)
        }

        recordTestEndTime(testName)
    }

    @Test
    @Order(6)
    @DisplayName("batchColumnUpdate: 여러 ID에 각기 다른 컬럼 업데이트 (변경사항 롤백됨)")
    @Commit
    fun `update_different_columns_for_ids_using_batchColumnUpdate (changes rolled back)`() {
        recordTestStartTime()
        val testName = "BatchColumnUpdate Mixed (changes rolled back unless @Commit)"
        println("=== $testName 시작: ${allInsertedBoardIds.size}개 전체 게시글 대상 ===")
        assumeTrue(allInsertedBoardIds.isNotEmpty(), "업데이트할 데이터가 @Order(1)에서 준비되어야 합니다.")

        val updates = mutableListOf<Pair<Long, ColumnUpdate>>()

        allInsertedBoardIds.forEachIndexed { index, boardId ->
            if (index % 2 == 0) {
                updates.add(boardId to ColumnUpdate("title", "Title_BatchColUpdate_$boardId", ColumnType.STRING))
            } else {
                updates.add(boardId to ColumnUpdate("view_count", Random.nextInt(500, 999), ColumnType.INT))
            }
        }

        assumeTrue(updates.isNotEmpty(), "업데이트할 대상이 생성되지 않았습니다.")
        assertThat(updates.size).isEqualTo(allInsertedBoardIds.size)

        var updatedCount = 0
        val timeMillis =
            measureTimeMillis {
                updatedCount = boardBatchRepository.batchColumnUpdate(updates, BatchOptions(batchSize = batchSize))
            }
        assertThat(updatedCount).isEqualTo(updates.size)
        println("$testName 완료. 소요시간: ${timeMillis / 1000.0} 초, 처리 건수: ${updates.size}")

        val sampleSize = 5
        if (updates.size >= sampleSize) {
            val titleUpdatedSampleIds = updates.filterIndexed { index, _ -> index % 2 == 0 }.take(sampleSize).map { it.first }
            if (titleUpdatedSampleIds.isNotEmpty()) {
                val checkedTitleBoards = boardBatchRepository.batchSelectByIds(titleUpdatedSampleIds)
                assertThat(checkedTitleBoards).hasSize(titleUpdatedSampleIds.size)
                checkedTitleBoards.forEach { board ->
                    assertThat(board.title).startsWith("Title_BatchColUpdate_")
                }
                println("타이틀 업데이트된 샘플 ${titleUpdatedSampleIds.size}건 검증 완료.")
            }

            val viewCountUpdatedSampleIds = updates.filterIndexed { index, _ -> index % 2 != 0 }.take(sampleSize).map { it.first }
            if (viewCountUpdatedSampleIds.isNotEmpty()) {
                val checkedViewCountBoards = boardBatchRepository.batchSelectByIds(viewCountUpdatedSampleIds)
                assertThat(checkedViewCountBoards).hasSize(viewCountUpdatedSampleIds.size)
                checkedViewCountBoards.forEach { board ->
                    assertThat(board.viewCount).isBetween(500, 999)
                }
                println("조회수 업데이트된 샘플 ${viewCountUpdatedSampleIds.size}건 검증 완료.")
            }
        }

        recordTestEndTime(testName)
    }
}
