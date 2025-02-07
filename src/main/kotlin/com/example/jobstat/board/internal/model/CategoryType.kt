package com.example.jobstat.board.internal.model

enum class CategoryType(
    val displayName: String,
    val description: String,
) {
    NOTICE("공지사항", "공식 공지사항"),
    FREE("자유게시판", "자유로운 주제의 게시판"),
    QNA("질문과답변", "질문과 답변을 위한 게시판"),
    JOBS("채용정보", "채용 관련 정보 게시판"),
}
