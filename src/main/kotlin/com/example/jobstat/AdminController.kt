package com.example.jobstat

import com.example.jobstat.core.constants.RestConstants
import com.example.jobstat.core.security.annotation.AdminAuth
import org.springframework.cache.CacheManager
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/${RestConstants.Versions.V1}/admin")
@AdminAuth // 이 컨트롤러의 모든 엔드포인트는 ADMIN 권한이 필요합니다.
class AdminController(
    private val cacheManager: CacheManager,
) {
    @PostMapping("/clear-cache")
    fun clearCaches(): ResponseEntity<Void> {
        cacheManager.getCache("StatsByEntityIdAndBaseDate")?.clear()
        cacheManager.getCache("statsWithRanking")?.clear()
        return ResponseEntity.ok().build()
    }
}
