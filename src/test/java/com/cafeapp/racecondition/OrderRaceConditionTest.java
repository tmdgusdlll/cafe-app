package com.cafeapp.racecondition;

import com.cafeapp.domain.menu.entity.Menu;
import com.cafeapp.domain.menu.entity.MenuStatus;
import com.cafeapp.domain.menu.repository.MenuRepository;
import com.cafeapp.domain.order.dto.request.OrderRequest;
import com.cafeapp.domain.order.repository.OrderRepository;
import com.cafeapp.domain.order.service.OrderService;
import com.cafeapp.domain.order.service.RedissonService;
import com.cafeapp.domain.pointTransaction.repository.PointTransactionRepository;
import com.cafeapp.domain.user.entity.User;
import com.cafeapp.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
public class OrderRaceConditionTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private PointTransactionRepository pointTransactionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RedissonService redissonService;

    private Long userId;
    private Long menuId;

    public static final Long INITIAL_POINT = 1000000L;

    @BeforeEach
    void setUp() {
        // 테스트용 유저 생성 (포인트 10000)
        User user = new User("테스트유저", INITIAL_POINT);
        userId = userRepository.save(user).getId();

        // 테스트용 메뉴 조회 (기존 메뉴 사용)
        menuId = 1L;
    }

    @AfterEach
    @Transactional
    void tearDown() {
        // 테스트 후 생성한 유저 삭제
        pointTransactionRepository.deleteAllByUserId(userId);
        orderRepository.deleteAllByUserId(userId);
        userRepository.deleteById(userId);
    }

    // 락 없는 버전
    private int runConcurrentTest(
            Long userId,
            Consumer<Long> task,
            String lockType
    ) throws InterruptedException {

        int poolSize = 32;
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(poolSize);
        CyclicBarrier barrier = new CyclicBarrier(poolSize);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        long start = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    try {
                        barrier.await(5, TimeUnit.SECONDS);
                    } catch (BrokenBarrierException | TimeoutException ignored) {}

                    task.accept(userId);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    System.out.println(Thread.currentThread().getName()
                            + " 실패: " + e.getMessage());
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long end = System.currentTimeMillis();
        executorService.shutdown();

        User user = userRepository.findById(userId).orElseThrow();

        // 정상적으로 차감됐다면 나와야 할 포인트
        long properlyDeductedPoint = INITIAL_POINT - (successCount.get() * 4500L);

        System.out.println("\n===== [" + lockType + "] 결과 =====");
        System.out.println("총 요청 수             : " + threadCount);
        System.out.println("성공 건수              : " + successCount.get());
        System.out.println("실패 건수              : " + failCount.get());
        System.out.println("초기 포인트            : " + INITIAL_POINT);
        System.out.println("차감될 포인트           : " + successCount.get() * 4500L);
        System.out.println("정상 차감 시 예상 포인트 : " + properlyDeductedPoint);
        System.out.println("실제 남은 포인트         : " + user.getPoint());
        System.out.println("정합성 여부            : " +
                (user.getPoint() == properlyDeductedPoint ? "✅ 정상" : "💥 불일치"));
        System.out.println("소요 시간              : " + (end - start) + "ms");
        System.out.println("==============================\n");

        return successCount.get();
    }

    // 락 있는 버전
    private int runConcurrentTestWithLock(
            Long userId,
            Consumer<Long> task,
            String lockType
    ) throws InterruptedException {

        int poolSize = 32;
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(poolSize);
        CyclicBarrier barrier = new CyclicBarrier(poolSize);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        long start = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    try {
                        barrier.await(5, TimeUnit.SECONDS);
                    } catch (BrokenBarrierException | TimeoutException ignored) {}

                    task.accept(userId);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    System.out.println(Thread.currentThread().getName()
                            + " 실패: " + e.getMessage());
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long end = System.currentTimeMillis();
        executorService.shutdown();

        User user = userRepository.findById(userId).orElseThrow();
        long expectedPoint = INITIAL_POINT - (successCount.get() * 4500L);

        System.out.println("\n===== [" + lockType + "] 결과 =====");
        System.out.println("총 요청 수    : " + threadCount);
        System.out.println("성공 건수     : " + successCount.get());
        System.out.println("실패 건수     : " + failCount.get());
        System.out.println("초기 포인트   : " + INITIAL_POINT);
        System.out.println("차감될 포인트    : " + successCount.get() * 4500L);
        System.out.println("예상 남은 포인트   : " + expectedPoint);
        System.out.println("실제 남은 포인트   : " + user.getPoint());
        System.out.println("정합성 여부   : " + (user.getPoint() == expectedPoint ? "✅ 정상" : "💥 불일치"));
        System.out.println("소요 시간     : " + (end - start) + "ms");
        System.out.println("==============================\n");

        return successCount.get();
    }

    @Test
    @DisplayName("락 없이 동시 N번 주문 시 포인트 정합성이 깨진다")
    void 락없이_동시_주문_포인트_정합성_깨짐() throws InterruptedException {
        // when
        int successCount = runConcurrentTest(
                userId,
                (id) -> orderService.orderAndPay(id, new OrderRequest(menuId, 1)),
                "락 없음"
        );

        // then
        User user = userRepository.findById(userId).orElseThrow();
        long expectedPoint = INITIAL_POINT - (successCount * 4500L);

        System.out.println("정합성 깨짐 여부: " + (user.getPoint() != expectedPoint ? "💥 불일치" : "✅ 정상"));

        // 락 없으면 정합성이 깨져야 함
        assertThat(user.getPoint()).isNotEqualTo(expectedPoint);
    }

    @Test
    @DisplayName("비관적 락 - 동시에 N번 주문 시 포인트가 정확히 차감되어야 한다")
    void 동시_주문_포인트_정합성() throws InterruptedException {
        int successCount = runConcurrentTestWithLock(
                userId,
                (id) -> orderService.orderAndPay(id, new OrderRequest(menuId, 1)),
                "비관적 락"
        );

        // then
        User user = userRepository.findById(userId).orElseThrow();
        long expectedPoint = INITIAL_POINT - (successCount * 4500L);

        assertThat(user.getPoint()).isEqualTo(expectedPoint);
    }

    @Test
    @DisplayName("분산 락 - 동시 10번 주문 시 포인트가 정확히 차감되어야 한다")
    void 분산락_동시_주문_포인트_정합성() throws InterruptedException {
        int successCount = runConcurrentTestWithLock(
                userId,
                (id) -> redissonService.orderAndPayWithRedisson(id, new OrderRequest(menuId, 1)),
                "분산 락"
        );

        User user = userRepository.findById(userId).orElseThrow();
        long expectedPoint = INITIAL_POINT - ((long) successCount * 4500L);

        assertThat(user.getPoint()).isEqualTo(expectedPoint);
    }

    @Test
    @DisplayName("비관적 락 - 재고 1개 메뉴에 N명이 동시 주문 시 정확히 1건만 성공하고 재고가 0이 된다")
    void 재고_동시성_정합성() throws InterruptedException {
        int threadCount = 10;
        Menu stockOneMenu = menuRepository.save(new Menu("재고1개메뉴", 1000L, 1, MenuStatus.ON_SALE));
        List<Long> testUserIds = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            testUserIds.add(userRepository.save(new User("재고테스트유저" + i, 10000L)).getId());
        }

        try {
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CyclicBarrier barrier = new CyclicBarrier(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                final Long uid = testUserIds.get(i);
                executorService.submit(() -> {
                    try {
                        barrier.await(5, TimeUnit.SECONDS);
                        orderService.orderAndPay(uid, new OrderRequest(stockOneMenu.getId(), 1));
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        System.out.println(Thread.currentThread().getName() + " 실패: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executorService.shutdown();

            System.out.println("\n===== [재고 동시성] 결과 =====");
            System.out.println("총 요청 수  : " + threadCount);
            System.out.println("성공 건수   : " + successCount.get());
            System.out.println("정합성 여부 : " + (successCount.get() == 1 ? "✅ 정상" : "💥 불일치"));
            System.out.println("==============================\n");

            assertThat(successCount.get()).isEqualTo(1);

            Menu updated = menuRepository.findById(stockOneMenu.getId()).orElseThrow();
            assertThat(updated.getStock()).isEqualTo(0);
            assertThat(updated.getStatus()).isEqualTo(MenuStatus.SOLD_OUT);

        } finally {
            for (Long uid : testUserIds) {
                pointTransactionRepository.deleteAllByUserId(uid);
                orderRepository.deleteAllByUserId(uid);
                userRepository.deleteById(uid);
            }
            menuRepository.deleteById(stockOneMenu.getId());
        }
    }

    @Test
    @DisplayName("[측정] 락 없이 재고 차감 시 오버셀(초과 판매) 발생 횟수 측정")
    void 락없는_재고차감_오버셀_측정() throws InterruptedException {
        int initialStock = 10;
        int threadCount = 100;
        int poolSize = 32;

        Menu testMenu = menuRepository.save(new Menu("오버셀측정메뉴", 1000L, initialStock, MenuStatus.ON_SALE));

        ExecutorService executorService = Executors.newFixedThreadPool(poolSize);
        CyclicBarrier barrier = new CyclicBarrier(poolSize);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        try {
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        try {
                            barrier.await(5, TimeUnit.SECONDS);
                        } catch (BrokenBarrierException | TimeoutException ignored) {}

                        // 락 없이 조회 → 재고 확인 → 차감 → 저장 (check-then-act, 의도적으로 비관적 락 미적용)
                        Menu menu = menuRepository.findById(testMenu.getId()).orElseThrow();
                        if (menu.getStock() > 0) {
                            menu.decreaseStock(1);
                            menuRepository.save(menu);
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        System.out.println(Thread.currentThread().getName() + " 실패: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executorService.shutdown();

            Menu finalMenu = menuRepository.findById(testMenu.getId()).orElseThrow();
            int oversoldCount = Math.max(0, successCount.get() - initialStock);

            System.out.println("\n===== [락 없음 - 오버셀 측정] 결과 =====");
            System.out.println("초기 재고            : " + initialStock);
            System.out.println("총 요청 수           : " + threadCount);
            System.out.println("성공(차감 처리) 건수  : " + successCount.get());
            System.out.println("최종 재고(DB)         : " + finalMenu.getStock());
            System.out.println("오버셀 발생 건수      : " + oversoldCount + " / " + threadCount);
            System.out.println("=======================================\n");

        } finally {
            menuRepository.deleteById(testMenu.getId());
        }
    }
}
