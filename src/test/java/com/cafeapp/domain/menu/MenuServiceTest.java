package com.cafeapp.domain.menu;

import com.cafeapp.common.config.redis.CacheManagerConfig;
import com.cafeapp.domain.MenuRanking.dto.PopularMenuResponse;
import com.cafeapp.domain.MenuRanking.service.MenuRankingService;
import com.cafeapp.domain.menu.dto.response.GetAllMenuResponse;
import com.cafeapp.domain.menu.entity.Menu;
import com.cafeapp.domain.menu.entity.MenuStatus;
import com.cafeapp.domain.menu.repository.MenuRepository;
import com.cafeapp.domain.menu.service.MenuService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
class MenuServiceTest {

    @Autowired
    private MenuService menuService;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private MenuRankingService menuRankingService;

    @Autowired
    private CacheManager redisCacheManager;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final List<Long> testMenuIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        clearRankingData();
        redisCacheManager.getCache(CacheManagerConfig.CACHE_NAME).clear();
    }

    @AfterEach
    void tearDown() {
        clearRankingData();
        redisCacheManager.getCache(CacheManagerConfig.CACHE_NAME).clear();
        testMenuIds.forEach(menuRepository::deleteById);
        testMenuIds.clear();
    }

    private void clearRankingData() {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 8; i++) {
            stringRedisTemplate.delete("menu:ranking:" + today.minusDays(i));
        }
        stringRedisTemplate.delete("menu:ranking:temp:" + today);
    }

    private Long saveTestMenu(String name, Long price, int stock, MenuStatus status) {
        Long id = menuRepository.save(new Menu(name, price, stock, status)).getId();
        testMenuIds.add(id);
        return id;
    }

    // ===================== 메뉴 목록 조회 =====================

    @Test
    @DisplayName("판매 중인 메뉴만 반환된다")
    void 판매중인_메뉴만_반환된다() {
        Long onSaleId = saveTestMenu("테스트판매중메뉴", 4500L, 10, MenuStatus.ON_SALE);
        Long soldOutId = saveTestMenu("테스트품절메뉴", 5000L, 0, MenuStatus.SOLD_OUT);

        List<GetAllMenuResponse> menus = menuService.getAllMenu();
        List<Long> ids = menus.stream().map(GetAllMenuResponse::getMenuId).toList();

        assertThat(ids).contains(onSaleId);
        assertThat(ids).doesNotContain(soldOutId);
    }

    @Test
    @DisplayName("SOLD_OUT 메뉴는 목록에 포함되지 않는다")
    void SOLD_OUT_메뉴는_반환되지_않는다() {
        saveTestMenu("테스트품절확인메뉴", 5000L, 0, MenuStatus.SOLD_OUT);

        List<GetAllMenuResponse> menus = menuService.getAllMenu();

        assertThat(menus).noneMatch(m -> m.getMenuName().equals("테스트품절확인메뉴"));
    }

    // ===================== 인기 메뉴 조회 =====================

    @Test
    @DisplayName("최근 7일 인기 메뉴 Top 3가 주문 횟수 내림차순으로 반환된다")
    void 인기메뉴_Top3_주문횟수_내림차순_반환() {
        Long menuId1 = saveTestMenu("랭킹1위메뉴", 4500L, 300, MenuStatus.ON_SALE);
        Long menuId2 = saveTestMenu("랭킹2위메뉴", 5000L, 300, MenuStatus.ON_SALE);
        Long menuId3 = saveTestMenu("랭킹3위메뉴", 5500L, 300, MenuStatus.ON_SALE);

        LocalDate today = LocalDate.now();
        menuRankingService.increaseMenuRanking(menuId1, 30, today);
        menuRankingService.increaseMenuRanking(menuId2, 20, today);
        menuRankingService.increaseMenuRanking(menuId3, 10, today);

        List<PopularMenuResponse> result = menuService.getPopularMenus();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getRank()).isEqualTo(1);
        assertThat(result.get(1).getRank()).isEqualTo(2);
        assertThat(result.get(2).getRank()).isEqualTo(3);
        assertThat(result.get(0).getMenuId()).isEqualTo(menuId1);
        assertThat(result.get(1).getMenuId()).isEqualTo(menuId2);
        assertThat(result.get(2).getMenuId()).isEqualTo(menuId3);
        assertThat(result.get(0).getOrderCount()).isEqualTo(30L);
        assertThat(result.get(1).getOrderCount()).isEqualTo(20L);
        assertThat(result.get(2).getOrderCount()).isEqualTo(10L);
    }

    @Test
    @DisplayName("인기 메뉴가 없으면 빈 목록을 반환한다")
    void 인기메뉴_없으면_빈목록_반환() {
        List<PopularMenuResponse> result = menuService.getPopularMenus();

        assertThat(result).isEmpty();
    }
}
