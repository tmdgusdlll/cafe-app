package com.cafeapp.domain.user.service;

import com.cafeapp.domain.pointTransaction.entity.PointTransaction;
import com.cafeapp.domain.pointTransaction.repository.PointTransactionRepository;
import com.cafeapp.domain.user.dto.request.ChargePointRequest;
import com.cafeapp.domain.user.dto.response.ChargePointResponse;
import com.cafeapp.domain.user.entity.User;
import com.cafeapp.domain.user.exception.UserException;
import com.cafeapp.domain.user.repository.UserRepository;
import com.cafeapp.global.exception.CafeException;
import com.cafeapp.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PointTransactionRepository pointTransactionRepository;

    // 포인트 충전 API
    @Transactional
    public ChargePointResponse chargePoint(Long userId, ChargePointRequest request) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new UserException(ErrorCode.USER_NOT_FOUND)
        );
        // 포인트 충전
        user.chargePoint(request.getPoint());

        // 충전 이력 기록
        PointTransaction tx = PointTransaction.charge(user, request.getPoint(), user.getPoint());
        pointTransactionRepository.save(tx);

        return ChargePointResponse.from(user);
    }
}
