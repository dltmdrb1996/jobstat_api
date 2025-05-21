package com.example.jobstat.core.base.repository

import com.example.jobstat.statistics_read.core.core_mongo_base.model.SnapshotPeriod
import com.example.jobstat.statistics_read.core.core_model.BaseDate
import com.example.jobstat.utils.base.BatchOperationTestSupport
import com.example.jobstat.utils.dummy.TestTimeSeriesDocument
import com.example.jobstat.utils.dummy.TestTimeSeriesRepository
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.query.Query
import java.time.Instant
import kotlin.random.Random

@TestMethodOrder(OrderAnnotation::class)
@DisplayName("BaseTimeSeriesRepository 통합 테스트")
class BaseTimeSeriesRepositoryIntegrationTest : BatchOperationTestSupport() {
    @Autowired
    lateinit var testTimeSeriesRepository: TestTimeSeriesRepository

    private val totalRecords = 996
    private val batchSize = 100
    private val allRecords = mutableListOf<TestTimeSeriesDocument>()
    private var startTime: Long = 0
    private val performanceMetrics = hashMapOf<String, Double>()

    override fun cleanupTestData() {
        val recordIds = allRecords.mapNotNull { it.id }
        for (batch in recordIds.chunked(batchSize)) {
            testTimeSeriesRepository.bulkDelete(batch)
        }
    }

    @BeforeAll
    fun beforeAll() {
    }

    @AfterAll
    fun afterAll() {
        cleanupTestData()
        printExecutionSummary()
    }

    private fun createSnapshotPeriod(baseDate: String): SnapshotPeriod {
        val year = baseDate.substring(0, 4).toInt()
        val month = baseDate.substring(4, 6).toInt()

        val startDate = Instant.parse("$year-${month.toString().padStart(2, '0')}-01T00:00:00Z")

        // 다음 달의 첫날을 구한 후 1초를 빼서 해당 월의 마지막 날 23:59:59로 설정
        val nextMonth =
            if (month == 12) {
                "${year + 1}-01-01T00:00:00Z"
            } else {
                "$year-${(month + 1).toString().padStart(2, '0')}-01T00:00:00Z"
            }
        val endDate = Instant.parse(nextMonth).minusSeconds(1)

        return SnapshotPeriod(startDate, endDate)
    }

    private fun createRandomDocument(baseDate: String): TestTimeSeriesDocument {
        val categories = listOf("A", "B", "C", "D", "E")
        return TestTimeSeriesDocument(
            baseDate = baseDate,
            period = createSnapshotPeriod(baseDate),
            value = Random.nextDouble(0.0, 100.0),
            category = categories.random(),
        )
    }

    @Test
    @Order(1)
    @DisplayName("스냅샷 기간이 유효하게 생성된다")
    fun testSnapshotPeriodValidity() {
        val baseDate = "202401"
        val snapshotPeriod = createSnapshotPeriod(baseDate)

        Assertions.assertEquals(
            "2024-01-01T00:00:00Z",
            snapshotPeriod.startDate.toString(),
        )
        Assertions.assertEquals(
            "2024-01-31T23:59:59Z",
            snapshotPeriod.endDate.toString(),
        )
        Assertions.assertEquals(30L, snapshotPeriod.durationInDays)
    }

    @Test
    @Order(2)
    @DisplayName("대량의 데이터를 삽입할 수 있다")
    fun testBulkInsert() {
        startTime = System.currentTimeMillis()
        allRecords.clear()

        val baseDates =
            (1..12).map { month ->
                val monthStr = month.toString().padStart(2, '0')
                "2024$monthStr"
            }

        var totalInserted = 0
        for (baseDate in baseDates) {
            val records = (1..totalRecords / 12).map { createRandomDocument(baseDate) }
            val result = testTimeSeriesRepository.bulkInsert(records)
            totalInserted += result.size
            allRecords.addAll(result)
        }

        Assertions.assertEquals(totalRecords, totalInserted, "Records count mismatch")

        val sampleRecord = allRecords.first()
        Assertions.assertTrue(sampleRecord.period.durationInDays in 28..31)

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        log.debug("Bulk insert execution time: $timeSeconds seconds")
        performanceMetrics["bulk_insert"] = timeSeconds
    }

    @Test
    @Order(3)
    @DisplayName("기준일자로 데이터를 조회할 수 있다")
    fun testFindByBaseDate() {
        startTime = System.currentTimeMillis()

        val testBaseDate = BaseDate("202401")
        val records = testTimeSeriesRepository.findByBaseDate(testBaseDate)

        Assertions.assertNotNull(records)
        Assertions.assertTrue(records?.baseDate == testBaseDate.toString())
        records?.let {
            Assertions.assertEquals(
                "2024-01-01T00:00:00Z",
                it.period.startDate.toString(),
            )
            Assertions.assertEquals(
                "2024-01-31T23:59:59Z",
                it.period.endDate.toString(),
            )
        }

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        log.debug("Find by base date execution time: $timeSeconds seconds")
        performanceMetrics["find_by_base_date"] = timeSeconds
    }

    @Test
    @Order(4)
    @DisplayName("기간 내의 데이터를 조회할 수 있다")
    fun testFindByBaseDateBetween() {
        startTime = System.currentTimeMillis()

        val startDate = BaseDate("202401")
        val endDate = BaseDate("202403")
        val records = testTimeSeriesRepository.findByBaseDateBetween(startDate, endDate)

        Assertions.assertTrue(records.isNotEmpty())
        Assertions.assertTrue(
            records.all { it.baseDate >= startDate.toString() && it.baseDate <= endDate.toString() },
        )
        Assertions.assertTrue(
            records.all { it.period.durationInDays in 28..31 },
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        log.debug("Find by date range execution time: $timeSeconds seconds")
        performanceMetrics["find_by_date_range"] = timeSeconds
    }

    @Test
    @Order(5)
    @DisplayName("최신 데이터를 조회할 수 있다")
    fun testFindLatest() {
        startTime = System.currentTimeMillis()

        val latestRecord = testTimeSeriesRepository.findLatest()

        Assertions.assertNotNull(latestRecord)
        Assertions.assertEquals("202412", latestRecord?.baseDate)
        latestRecord?.let {
            Assertions.assertEquals(
                "2024-12-01T00:00:00Z",
                it.period.startDate.toString(),
            )
            Assertions.assertEquals(
                "2024-12-31T23:59:59Z",
                it.period.endDate.toString(),
            )
        }

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        log.debug("Find latest execution time: $timeSeconds seconds")
        performanceMetrics["find_latest"] = timeSeconds
    }

    @Test
    @Order(6)
    @DisplayName("최신 N개의 데이터를 조회할 수 있다")
    fun testFindLatestN() {
        startTime = System.currentTimeMillis()

        val n = 3
        val latestRecords = testTimeSeriesRepository.findLatestN(n)

        Assertions.assertEquals(n, latestRecords.size)
        Assertions.assertTrue(
            latestRecords[0].baseDate >= latestRecords[1].baseDate &&
                latestRecords[1].baseDate >= latestRecords[2].baseDate,
        )
        Assertions.assertTrue(
            latestRecords[0].period.startDate >= latestRecords[1].period.startDate &&
                latestRecords[1].period.startDate >= latestRecords[2].period.startDate,
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        log.debug("Find latest N execution time: $timeSeconds seconds")
        performanceMetrics["find_latest_n"] = timeSeconds
    }

    @Test
    @Order(7)
    @DisplayName("대량의 데이터를 삭제할 수 있다")
    fun testBulkDelete() {
        startTime = System.currentTimeMillis()

        val recordIds = allRecords.mapNotNull { it.id }
        var totalDeleted = 0

        for (batch in recordIds.chunked(batchSize)) {
            val deletedCount = testTimeSeriesRepository.bulkDelete(batch)
            totalDeleted += deletedCount
        }

        val remainingRecords = testTimeSeriesRepository.findAllByQuery(Query())
        Assertions.assertTrue(remainingRecords.isEmpty())

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        log.debug("Bulk delete execution time: $timeSeconds seconds")
        performanceMetrics["bulk_delete"] = timeSeconds
    }
}
