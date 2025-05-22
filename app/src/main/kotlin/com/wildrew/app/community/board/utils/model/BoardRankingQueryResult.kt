package com.wildrew.app.community.board.utils.model

interface BoardRankingQueryResult {
    val boardId: Long
    val score: Int
}

class BoardRankingQueryResultImpl(
    override val boardId: Long,
    override val score: Int,
) : BoardRankingQueryResult {
    companion object {
        fun create(
            boardId: Long,
            score: Int,
        ): BoardRankingQueryResult = BoardRankingQueryResultImpl(boardId, score)
    }
}
