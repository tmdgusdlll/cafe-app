package com.cafeapp.domain.menu.controller;

import com.cafeapp.global.response.ApiResponse;
import com.cafeapp.domain.MenuRanking.dto.PopularMenuResponse;
import com.cafeapp.domain.menu.dto.response.GetAllMenuResponse;
import com.cafeapp.domain.menu.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    // 커피 메뉴 목록 조회 API
    @GetMapping("/menus")
    public ResponseEntity<ApiResponse<List<GetAllMenuResponse>>> getAllMenu() {
        return ResponseEntity.ok(ApiResponse.success(menuService.getAllMenu()));
    }

    // 인기 top3 (7일치)
    @GetMapping("/menus/popular")
    public ResponseEntity<ApiResponse<List<PopularMenuResponse>>> getPopularMenus() {
        return ResponseEntity.ok(ApiResponse.success(menuService.getPopularMenus()));
    }
}
