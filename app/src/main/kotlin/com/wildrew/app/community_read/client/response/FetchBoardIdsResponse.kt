package com.wildrew.app.community_read.client.response

import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode

data class FetchBoardIdsResponse(
    val ids: List<String>,
    val hasNext: Boolean,
) {
    companion object {
        fun from(response: FetchBoardIdsResponse): List<Long> = response.ids.map { it.toLongOrNull() ?: throw AppException.fromErrorCode(
            ErrorCode.INVALID_ARGUMENT) }
    }
}
