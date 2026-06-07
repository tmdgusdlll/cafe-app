package com.cafeapp.domain.menu;

import com.cafeapp.common.config.redis.CacheManagerConfig;
import com.cafeapp.domain.MenuRanking.dto.PopularMenuResponse;
import com.cafeapp.domain.MenuRanking.service.MenuRankingService;
import com.cafeapp.domain.menu.dto.response.GetAllMenuResponse;
import com.cafeapp.domain.menu.entity.Menu;
import com.cafeapp.domain.menu.entity.MenuStatus;
import com.cafeapp.domain.menu.repository.MenuRepository;
import com.cafeapp.domain.menu.service.MenuService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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

    @Autowired
    private EntityManagerFactory entityManagerFactory;

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
    @DisplayName("캐시 적용 전후 메뉴 목록 조회 응답시간을 비교 측정한다")
    void 캐시_적용_전후_응답시간_비교() {
        for (int i = 0; i < 300; i++) {
            saveTestMenu("캐시측정메뉴" + i, 4500L, 10, MenuStatus.ON_SALE);
        }

        int warmup = 20;
        int iterations = 100;

        // JIT/커넥션 워밍업 (측정에서 제외)
        for (int i = 0; i < warmup; i++) {
            redisCacheManager.getCache(CacheManagerConfig.CACHE_NAME).clear();
            menuService.getAllMenu();
            menuService.getAllMenu();
        }

        long missTotalNanos = 0L;
        long hitTotalNanos = 0L;

        for (int i = 0; i < iterations; i++) {
            redisCacheManager.getCache(CacheManagerConfig.CACHE_NAME).clear();

            long missStart = System.nanoTime();
            menuService.getAllMenu();
            missTotalNanos += System.nanoTime() - missStart;

            long hitStart = System.nanoTime();
            menuService.getAllMenu();
            hitTotalNanos += System.nanoTime() - hitStart;
        }

        double avgMissMs = missTotalNanos / (double) iterations / 1_000_000.0;
        double avgHitMs = hitTotalNanos / (double) iterations / 1_000_000.0;

        System.out.printf("[캐시 MISS(DB 조회) 평균 응답시간] %.3f ms%n", avgMissMs);
        System.out.printf("[캐시 HIT(Redis 조회) 평균 응답시간] %.3f ms%n", avgHitMs);
        System.out.printf("[단축률] %.1f%%%n", (1 - avgHitMs / avgMissMs) * 100);
    }

    @Test
    @DisplayName("캐시 적용 시 동일 요청에 대해 DB 쿼리가 1회만 실행되는지 측정한다")
    void 캐시_적용_시_DB_쿼리_실행횟수_측정() {
        saveTestMenu("쿼리횟수측정메뉴", 4500L, 10, MenuStatus.ON_SALE);

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        int requestCount = 100;
        for (int i = 0; i < requestCount; i++) {
            menuService.getAllMenu();
        }

        long queryCount = statistics.getPrepareStatementCount();

        System.out.printf("[동일 요청 %d건에 대한 실제 DB 쿼리 실행 횟수] %d회%n", requestCount, queryCount);
        System.out.printf("[캐시로 절감된 DB 호출] %d회 (%.1f%% 절감)%n",
                requestCount - queryCount, (1 - queryCount / (double) requestCount) * 100);

        assertThat(queryCount).isEqualTo(1L);
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
