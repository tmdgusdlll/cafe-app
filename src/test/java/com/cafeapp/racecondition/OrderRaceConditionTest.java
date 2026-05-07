package com.cafeapp.racecondition;

import com.cafeapp.domain.menu.repository.MenuRepository;
import com.cafeapp.domain.order.dto.request.OrderRequest;
import com.cafeapp.domain.order.repository.OrderRepository;
import com.cafeapp.domain.order.service.OrderService;
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

    private Long userId;
    private Long menuId;

    @BeforeEach
    void setUp() {
        // 테스트용 유저 생성 (포인트 10000)
        User user = new User("테스트유저", 100000L);
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
        long properlyDeductedPoint = 100000L - (successCount.get() * 4500L);

        System.out.println("\n===== [" + lockType + "] 결과 =====");
        System.out.println("총 요청 수             : " + threadCount);
        System.out.println("성공 건수              : " + successCount.get());
        System.out.println("실패 건수              : " + failCount.get());
        System.out.println("초기 포인트            : " + 100000L);
        System.out.println("차감될 포인트           : " + successCount.get() * 4500L);
        System.out.println("정상 차감 시 예상 포인트 : " + properlyDeductedPoint);
        System.out.println("실제 남은 포인트         : " + user.getPoint());
        System.out.println("정합성 여부            : " +
                (user.getPoint() == properlyDeductedPoint ? "✅ 정상" : "💥 불일치"));
        System.out.println("소요 시간              : " + (end - start) + "ms");
        System.out.println("==============================\n");

        return successCount.get();
    }

    private int runConcurrentTestWithLock(
            Long userId,
            Consumer<Long> task,
            String lockType
    ) throws InterruptedException {

        int poolSize = 32;
        int threadCount = 10;
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
        long expectedPoint = 100000L - (successCount.get() * 4500L);

        System.out.println("\n===== [" + lockType + "] 결과 =====");
        System.out.println("총 요청 수    : " + threadCount);
        System.out.println("성공 건수     : " + successCount.get());
        System.out.println("실패 건수     : " + failCount.get());
        System.out.println("초기 포인트   : " + 100000L);
        System.out.println("차감될 포인트    : " + successCount.get() * 4500L);
        System.out.println("예상 남은 포인트   : " + expectedPoint);
        System.out.println("실제 남은 포인트   : " + user.getPoint());
        System.out.println("정합성 여부   : " + (user.getPoint() == expectedPoint ? "✅ 정상" : "💥 불일치"));
        System.out.println("소요 시간     : " + (end - start) + "ms");
        System.out.println("==============================\n");

        return successCount.get();
    }

    @Test
    @DisplayName("락 없이 동시 100번 주문 시 포인트 정합성이 깨진다")
    void 락없이_동시_주문_포인트_정합성_깨짐() throws InterruptedException {
        // when
        int successCount = runConcurrentTest(
                userId,
                (id) -> orderService.orderAndPay(id, new OrderRequest(menuId, 1)),
                "락 없음"
        );

        // then
        User user = userRepository.findById(userId).orElseThrow();
        long expectedPoint = 100000L - (successCount * 4500L);

        System.out.println("정합성 깨짐 여부: " + (user.getPoint() != expectedPoint ? "💥 불일치" : "✅ 정상"));

        // 락 없으면 정합성이 깨져야 함
        assertThat(user.getPoint()).isNotEqualTo(expectedPoint);
    }

    @Test
    @DisplayName("동시에 100번 주문 시 포인트가 정확히 차감되어야 한다")
    void 동시_주문_포인트_정합성() throws InterruptedException {
        int successCount = runConcurrentTestWithLock(
                userId,
                (id) -> orderService.orderAndPay(id, new OrderRequest(menuId, 1)),
                "비관적 락"
        );

        // then
        User user = userRepository.findById(userId).orElseThrow();
        long expectedPoint = 100000L - (successCount * 4500L);

        assertThat(user.getPoint()).isEqualTo(expectedPoint);
    }
}
