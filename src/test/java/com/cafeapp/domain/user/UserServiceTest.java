package com.cafeapp.domain.user;

import com.cafeapp.domain.pointTransaction.entity.PointTransaction;
import com.cafeapp.domain.pointTransaction.entity.PointTransactionType;
import com.cafeapp.domain.pointTransaction.repository.PointTransactionRepository;
import com.cafeapp.domain.user.dto.request.ChargePointRequest;
import com.cafeapp.domain.user.dto.response.ChargePointResponse;
import com.cafeapp.domain.user.entity.User;
import com.cafeapp.domain.user.exception.UserException;
import com.cafeapp.domain.user.repository.UserRepository;
import com.cafeapp.domain.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local")
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PointTransactionRepository pointTransactionRepository;

    private Long userId;

    @BeforeEach
    void setUp() {
        userId = userRepository.save(new User("포인트테스트유저", 0L)).getId();
    }

    @AfterEach
    void tearDown() {
        pointTransactionRepository.deleteAllByUserId(userId);
        userRepository.deleteById(userId);
    }

    private ChargePointRequest chargeRequest(long point) {
        ChargePointRequest request = new ChargePointRequest();
        ReflectionTestUtils.setField(request, "point", point);
        return request;
    }

    @Test
    @DisplayName("포인트 충전 성공 - 포인트가 정확히 증가한다")
    void 포인트_충전_성공() {
        ChargePointResponse response = userService.chargePoint(userId, chargeRequest(10000L));

        assertThat(response.getPoint()).isEqualTo(10000L);

        User updated = userRepository.findById(userId).orElseThrow();
        assertThat(updated.getPoint()).isEqualTo(10000L);
    }

    @Test
    @DisplayName("포인트 충전 성공 - 충전 이력(CHARGE 타입)이 저장된다")
    void 포인트_충전_이력_저장() {
        userService.chargePoint(userId, chargeRequest(5000L));

        List<PointTransaction> txList = pointTransactionRepository.findAllByUserId(userId);

        assertThat(txList).hasSize(1);
        assertThat(txList.get(0).getType()).isEqualTo(PointTransactionType.CHARGE);
        assertThat(txList.get(0).getAmount()).isEqualTo(5000L);
        assertThat(txList.get(0).getPointBalance()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("존재하지 않는 유저에게 충전 시 예외가 발생한다")
    void 존재하지_않는_유저_충전시_예외() {
        assertThatThrownBy(() -> userService.chargePoint(999999L, chargeRequest(10000L)))
                .isInstanceOf(UserException.class);
    }
}
