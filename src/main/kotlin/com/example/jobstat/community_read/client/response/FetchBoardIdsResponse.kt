package com.example.jobstat.community_read.client.response

import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode

data class FetchBoardIdsResponse(
    val ids: List<String>,
    val hasNext: Boolean,
) {
    companion object {
        fun from(response: FetchBoardIdsResponse): List<Long> = response.ids.map { it.toLongOrNull() ?: throw AppException.fromErrorCode(ErrorCode.INVALID_ARGUMENT) }
    }
}
