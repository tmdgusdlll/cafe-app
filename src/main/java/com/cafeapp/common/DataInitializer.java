package com.cafeapp.common;

import com.cafeapp.domain.menu.entity.Menu;
import com.cafeapp.domain.menu.entity.MenuStatus;
import com.cafeapp.domain.menu.repository.MenuRepository;
import com.cafeapp.domain.user.entity.User;
import com.cafeapp.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final MenuRepository menuRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        // 이미 데이터 있으면 스킵 (중복 방지)
        if (menuRepository.count() > 0) return;

        // 유저 3명
        userRepository.saveAll(List.of(
                new User("신형만",1000000L),
                new User("봉미선", 500000L),
                new User("짱구",0L)
        ));

        // 메뉴 10개
        menuRepository.saveAll(List.of(
                new Menu("아메리카노", 4500L, 300, MenuStatus.ON_SALE),
                new Menu("카페라떼", 5000L, 300, MenuStatus.ON_SALE),
                new Menu("카푸치노", 5500L, 300, MenuStatus.ON_SALE),
                new Menu("바닐라라떼", 5500L, 300, MenuStatus.ON_SALE),
                new Menu("카라멜마끼아또", 6000L, 300, MenuStatus.ON_SALE),
                new Menu("에스프레소", 4000L, 300, MenuStatus.ON_SALE),
                new Menu("콜드브루", 5000L, 300, MenuStatus.ON_SALE),
                new Menu("플랫화이트", 5500L, 300, MenuStatus.ON_SALE),
                new Menu("모카라떼", 5500L, 300, MenuStatus.ON_SALE),
                new Menu("헤이즐넛라떼", 6000L, 300, MenuStatus.ON_SALE),
                new Menu("두바이초코라떼", 8000L, 300, MenuStatus.SOLD_OUT),
                new Menu("딸기망고스무디", 6000L, 300, MenuStatus.SOLD_OUT)
        ));
    }
}