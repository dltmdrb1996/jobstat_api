package com.example.jobstat.community_read.repository

interface BoardCountRepository {
    fun createOrUpdate(categoryId: Long, count: Long)
    fun read(categoryId: Long): Long?
}