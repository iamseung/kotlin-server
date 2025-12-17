package kr.hhplus.be.server.api.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Ranking", description = "콘서트 랭킹")
class RankingController(

) {
}