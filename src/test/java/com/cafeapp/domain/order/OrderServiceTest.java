package com.cafeapp.domain.order;

import com.cafeapp.domain.menu.entity.Menu;
import com.cafeapp.domain.menu.entity.MenuStatus;
import com.cafeapp.domain.menu.exception.MenuException;
import com.cafeapp.domain.menu.repository.MenuRepository;
import com.cafeapp.domain.order.dto.request.OrderRequest;
import com.cafeapp.domain.order.dto.response.OrderResponse;
import com.cafeapp.domain.order.entity.OrderStatus;
import com.cafeapp.domain.order.exception.OrderException;
import com.cafeapp.domain.order.repository.OrderRepository;
import com.cafeapp.domain.order.service.OrderService;
import com.cafeapp.domain.pointTransaction.repository.PointTransactionRepository;
import com.cafeapp.domain.user.entity.User;
import com.cafeapp.domain.user.exception.UserException;
import com.cafeapp.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local")
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PointTransactionRepository pointTransactionRepository;

    private User testUser;
    private Menu testMenu;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(new User("주문테스트유저", 100000L));
        testMenu = menuRepository.save(new Menu("주문테스트메뉴", 4500L, 100, MenuStatus.ON_SALE));
    }

    @AfterEach
    void tearDown() {
        pointTransactionRepository.deleteAllByUserId(testUser.getId());
        orderRepository.deleteAllByUserId(testUser.getId());
        userRepository.deleteById(testUser.getId());
        menuRepository.deleteById(testMenu.getId());
    }

    @Test
    @DisplayName("정상 주문 성공 - 포인트 차감, 재고 차감, 주문 상태 PAYMENT_COMPLETED")
    void 정상_주문_성공() {
        OrderResponse response = orderService.orderAndPay(
                testUser.getId(), new OrderRequest(testMenu.getId(), 1));

        assertThat(response.getStatus()).isEqualTo(OrderStatus.PAYMENT_COMPLETED);

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getPoint()).isEqualTo(100000L - 4500L);

        Menu updatedMenu = menuRepository.findById(testMenu.getId()).orElseThrow();
        assertThat(updatedMenu.getStock()).isEqualTo(99);
    }

    @Test
    @DisplayName("포인트 부족 시 주문이 실패한다")
    void 포인트_부족시_주문_실패() {
        User poorUser = userRepository.save(new User("포인트없는유저", 0L));

        try {
            assertThatThrownBy(() -> orderService.orderAndPay(
                    poorUser.getId(), new OrderRequest(testMenu.getId(), 1)))
                    .isInstanceOf(UserException.class);
        } finally {
            userRepository.deleteById(poorUser.getId());
        }
    }

    @Test
    @DisplayName("재고 부족 시 주문이 실패한다")
    void 재고_부족시_주문_실패() {
        Menu noStockMenu = menuRepository.save(new Menu("재고없는메뉴", 4500L, 0, MenuStatus.ON_SALE));

        try {
            assertThatThrownBy(() -> orderService.orderAndPay(
                    testUser.getId(), new OrderRequest(noStockMenu.getId(), 1)))
                    .isInstanceOf(MenuException.class);
        } finally {
            menuRepository.deleteById(noStockMenu.getId());
        }
    }

    @Test
    @DisplayName("SOLD_OUT 메뉴 주문 시 실패한다")
    void SOLD_OUT_메뉴_주문_실패() {
        Menu soldOutMenu = menuRepository.save(new Menu("품절메뉴", 4500L, 0, MenuStatus.SOLD_OUT));

        try {
            assertThatThrownBy(() -> orderService.orderAndPay(
                    testUser.getId(), new OrderRequest(soldOutMenu.getId(), 1)))
                    .isInstanceOf(OrderException.class);
        } finally {
            menuRepository.deleteById(soldOutMenu.getId());
        }
    }

    @Test
    @DisplayName("존재하지 않는 메뉴 주문 시 실패한다")
    void 존재하지_않는_메뉴_주문_실패() {
        assertThatThrownBy(() -> orderService.orderAndPay(
                testUser.getId(), new OrderRequest(999999L, 1)))
                .isInstanceOf(OrderException.class);
    }

    @Test
    @DisplayName("재고 마지막 1개 소진 시 메뉴 상태가 자동으로 SOLD_OUT으로 변경된다")
    void 재고_마지막1개_소진시_SOLD_OUT_자동변경() {
        Menu lastStockMenu = menuRepository.save(new Menu("재고1개메뉴", 4500L, 1, MenuStatus.ON_SALE));

        try {
            orderService.orderAndPay(testUser.getId(), new OrderRequest(lastStockMenu.getId(), 1));

            Menu updated = menuRepository.findById(lastStockMenu.getId()).orElseThrow();
            assertThat(updated.getStock()).isEqualTo(0);
            assertThat(updated.getStatus()).isEqualTo(MenuStatus.SOLD_OUT);
        } finally {
            pointTransactionRepository.deleteAllByUserId(testUser.getId());
            orderRepository.deleteAllByUserId(testUser.getId());
            menuRepository.deleteById(lastStockMenu.getId());
        }
    }
}
