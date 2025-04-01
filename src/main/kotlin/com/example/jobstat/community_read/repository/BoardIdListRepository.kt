package com.example.jobstat.community_read.repository

interface BoardIdListRepository {
    // 기본 ID 목록 (최신순)
    fun add(boardId: Long, sortValue: Double = System.currentTimeMillis().toDouble(), limit: Long = 1000)
    fun delete(boardId: Long)
    fun readAllByTime(offset: Long, limit: Long): List<Long>
    fun readAllByTimeInfiniteScroll(lastBoardId: Long?, limit: Long): List<Long>

    // 카테고리별 ID 목록
    fun addToCategoryList(categoryId: Long, boardId: Long, sortValue: Double = System.currentTimeMillis().toDouble())
    fun deleteFromCategoryList(categoryId: Long, boardId: Long)
    fun readAllByCategory(categoryId: Long, offset: Long, limit: Long): List<Long>
    fun readAllByCategoryInfiniteScroll(categoryId: Long, lastBoardId: Long?, limit: Long): List<Long>

    // 기간별 좋아요순 ID 목록
    fun addToLikesByDayList(boardId: Long, createdAt: Long, likeCount: Int)
    fun addToLikesByWeekList(boardId: Long, createdAt: Long, likeCount: Int)
    fun addToLikesByMonthList(boardId: Long, createdAt: Long, likeCount: Int)
    fun deleteFromLikesList(boardId: Long)
    fun readAllByLikesDay(limit: Long = 100): List<Long>
    fun readAllByLikesWeek(limit: Long = 100): List<Long>
    fun readAllByLikesMonth(limit: Long = 100): List<Long>

    // 기간별 조회수순 ID 목록
    fun addToViewsByDayList(boardId: Long, createdAt: Long, viewCount: Int)
    fun addToViewsByWeekList(boardId: Long, createdAt: Long, viewCount: Int)
    fun addToViewsByMonthList(boardId: Long, createdAt: Long, viewCount: Int)
    fun deleteFromViewsList(boardId: Long)
    fun readAllByViewsDay(limit: Long = 100): List<Long>
    fun readAllByViewsWeek(limit: Long = 100): List<Long>
    fun readAllByViewsMonth(limit: Long = 100): List<Long>
}


