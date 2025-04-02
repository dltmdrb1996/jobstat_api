package com.example.jobstat.core.outbox

import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.outbox.OutboxConstants
import com.example.jobstat.core.event.outbox.OutboxEventPublisher
import com.example.jobstat.core.event.payload.board.BoardCreatedEventPayload
import com.example.jobstat.core.global.utils.serializer.DataSerializer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.context.ApplicationEventPublisher
import kotlin.test.assertEquals

@DisplayName("OutboxEventPublisher 테스트")
class OutboxEventPublisherTest {
    private lateinit var applicationEventPublisher: ApplicationEventPublisher
    private lateinit var dataSerializer: DataSerializer
    private lateinit var outboxEventPublisher: OutboxEventPublisher
    
    @BeforeEach
    fun setUp() {
        applicationEventPublisher = mock()
        dataSerializer = mock()
        outboxEventPublisher = OutboxEventPublisher(applicationEventPublisher, dataSerializer)
        
        // dataSerializer가 호출될 때 JSON 문자열을 반환하도록 설정
        whenever(dataSerializer.serialize(any())).thenReturn("{\"test\":\"value\"}")
    }
    
    @Nested
    @DisplayName("이벤트 발행")
    inner class PublishEvent {
        
        @Test
        @DisplayName("게시글 생성 이벤트를 발행할 수 있다")
        fun publishBoardCreatedEvent() {
            // given
            val eventType = EventType.BOARD_CREATED
            val payload = BoardCreatedEventPayload(
                boardId = 123L,
                title = "테스트 게시글",
                author = "테스트 작성자",
                categoryId = 1L,
                userId = 100L
            )
            val shardKey = 123L
            
            // when
            outboxEventPublisher.publish(eventType, payload, shardKey)
            
            // then
            val outboxCaptor = argumentCaptor<OutboxEvent>()
            verify(applicationEventPublisher, times(1)).publishEvent(outboxCaptor.capture())
            
            val capturedOutbox = outboxCaptor.firstValue.outbox
            assertEquals(eventType, capturedOutbox.eventType)
            assertEquals(shardKey % OutboxConstants.SHARD_COUNT, capturedOutbox.shardKey)
        }
        
        @Test
        @DisplayName("샤딩 키는 SHARD_COUNT로 나눈 나머지가 사용된다")
        fun shardKeyIsModulo() {
            // given
            val eventType = EventType.BOARD_CREATED
            val payload = BoardCreatedEventPayload(
                boardId = 123L,
                title = "테스트 게시글",
                author = "테스트 작성자",
                categoryId = 1L,
                userId = 100L
            )
            val shardKey = 1234567L
            
            // when
            outboxEventPublisher.publish(eventType, payload, shardKey)
            
            // then
            val outboxCaptor = argumentCaptor<OutboxEvent>()
            verify(applicationEventPublisher, times(1)).publishEvent(outboxCaptor.capture())
            
            val capturedOutbox = outboxCaptor.firstValue.outbox
            assertEquals(shardKey % OutboxConstants.SHARD_COUNT, capturedOutbox.shardKey)
        }
        
        @Test
        @DisplayName("다양한 이벤트 타입을 발행할 수 있다")
        fun publishDifferentEventTypes() {
            // given
            val eventTypes = listOf(
                EventType.BOARD_CREATED,
                EventType.COMMENT_CREATED,
                EventType.BOARD_LIKED
            )
            
            val payload = BoardCreatedEventPayload(
                boardId = 123L,
                title = "테스트 게시글",
                author = "테스트 작성자",
                categoryId = 1L,
                userId = 100L
            )
            
            // when
            eventTypes.forEach { eventType ->
                outboxEventPublisher.publish(eventType, payload, 123L)
            }
            
            // then
            verify(applicationEventPublisher, times(eventTypes.size)).publishEvent(any())
        }
    }
} 