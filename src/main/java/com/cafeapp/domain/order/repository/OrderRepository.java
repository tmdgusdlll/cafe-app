package com.cafeapp.domain.order.repository;

import com.cafeapp.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM Order o WHERE o.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
