package com.cafeapp.domain.MenuRanking.controller;

import com.cafeapp.domain.MenuRanking.service.MenuRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MenuRankingController {

    private final MenuRankingService menuRankingService;


    // 인기 미뉴 목록 조회
//    @GetMapping("/menus/popular")
//    public ResponseEntity<ApiResponse<List<MenuScoreDto>>> getMenuRankingTop3In7Days() {
//        return ResponseEntity.ok(ApiResponse.success(menuRankingService.getMenuRankingTop3In7Days()));
//    }
}
