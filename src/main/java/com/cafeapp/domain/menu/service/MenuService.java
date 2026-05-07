package com.cafeapp.domain.menu.service;

import com.cafeapp.domain.MenuRanking.dto.PopularMenuResponse;
import com.cafeapp.domain.MenuRanking.service.MenuRankingService;
import com.cafeapp.domain.menu.dto.response.GetAllMenuResponse;
import com.cafeapp.domain.menu.dto.response.MenuScoreDto;
import com.cafeapp.domain.menu.entity.Menu;
import com.cafeapp.domain.menu.entity.MenuStatus;
import com.cafeapp.domain.menu.exception.MenuException;
import com.cafeapp.domain.menu.repository.MenuRepository;
import com.cafeapp.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;
    private final MenuRankingService menuRankingService;

    // 커피 메뉴 목록 조회
    public List<GetAllMenuResponse> getAllMenu() {
        return menuRepository.findAllByStatus(MenuStatus.ON_SALE).stream()
                .map(GetAllMenuResponse::from)
                .toList();
    }

    public List<PopularMenuResponse> getPopularMenus() {
        List<MenuScoreDto> rankingData = menuRankingService.getMenuRankingTop3In7Days();

        if (rankingData.isEmpty()) {
            return Collections.emptyList();
        }

        // 랭킹에 있는 메뉴ID들만 추출
        List<Long> menuIds = rankingData.stream()
                .map(MenuScoreDto::menuId)
                .toList();

        // DB쿼리 1번으로 메뉴 정보 조회 (IN쿼리)
        Map<Long, Menu> menuMap = menuRepository.findAllById(menuIds).stream()
                .collect(Collectors.toMap(Menu::getId, menu -> menu));

        // Redis 순위 데이터와 DB 메뉴 정보를 조합하여 최종 응답 생성
        return IntStream.range(0, rankingData.size())
                .mapToObj(i -> {
                    MenuScoreDto scoreDto = rankingData.get(i);
                    Menu menu = menuMap.get(scoreDto.menuId());

                    if (menu == null) throw new MenuException(ErrorCode.MENU_NOT_FOUND);

                    return new PopularMenuResponse(
                            i + 1,
                            menu.getId(),
                            menu.getMenuName(),
                            scoreDto.orderCount()
                    );
                })
                .toList();
    }
}
