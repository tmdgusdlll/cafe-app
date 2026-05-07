package com.cafeapp.domain.MenuRanking.service;

import com.cafeapp.domain.MenuRanking.dto.PopularMenuResponse;
import com.cafeapp.domain.menu.dto.response.MenuScoreDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class MenuRankingService {

    private final StringRedisTemplate stringRedisTemplate;
    private static final String MENU_RANKING_KEY = "menu:ranking:";

    // 결제완료된 정보를 기준으로 판매된 커피의 랭킹 카운트 올려주는 메서드
    public void increaseMenuRanking(Long menuId, int quantity, LocalDate currentDate) {
        // "menu:ranking:2025-05-07" 키 생성
        String key = MENU_RANKING_KEY + currentDate.toString();

        // 해당 날짜 키의 ZSET에서 menuId의 score를 quantity만큼 증가
        // ex) 아메리카노(menuId=1)를 2잔 주문 → score 2 증가
        stringRedisTemplate.opsForZSet().incrementScore(key, String.valueOf(menuId), quantity);

        // 8일 뒤에 자동 삭제
        stringRedisTemplate.expire(key, 8, TimeUnit.DAYS);
    }

    public List<MenuScoreDto> getMenuRankingTop3In7Days() {
        LocalDate currentDate = LocalDate.now();

        // 7일치 합산할 임시 키
        String tempKey = "menu:ranking:temp:" + currentDate.toString();

        // 이미 7일치 합살할 임시 키가 존재한다면 UNION 연산 생략
        Boolean hasKey = stringRedisTemplate.hasKey(tempKey);

        if (Boolean.FALSE.equals(hasKey)) {
            List<String> keys = IntStream.range(0, 7)
                    .mapToObj(i -> MENU_RANKING_KEY + currentDate.minusDays(i).toString())
                    .toList();

            // ZUNIONSTORE로 7일치 합산
            // keys.get(0) = 오늘 키 (기준)
            // keys.subList(1, 7) = 나머지 6일 키
            // 7개 키를 전부 합산해서 tempKey에 저장
            stringRedisTemplate.opsForZSet()
                    .unionAndStore(keys.get(0), keys.subList(1, keys.size()), tempKey);

            // 10분마다 갱신
            stringRedisTemplate.expire(tempKey, 10, TimeUnit.MINUTES);
        }

        // top3 조회
        Set<TypedTuple<String>> result = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(tempKey, 0, 2);

        if (result == null) {
            return Collections.emptyList();
        }
        return result.stream()
                .map(tuple -> new MenuScoreDto(
                        Long.parseLong(tuple.getValue()),   // menuId
                        tuple.getScore().longValue()    // orderCount
                ))
                .toList();
    }
}
