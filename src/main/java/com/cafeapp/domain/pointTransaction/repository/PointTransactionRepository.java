package com.cafeapp.domain.pointTransaction.repository;

import com.cafeapp.domain.pointTransaction.entity.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    @Query("SELECT pt FROM PointTransaction pt WHERE pt.user.id = :userId")
    List<PointTransaction> findAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PointTransaction pt WHERE pt.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
