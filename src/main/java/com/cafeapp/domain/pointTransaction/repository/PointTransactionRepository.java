package com.cafeapp.domain.pointTransaction.repository;

import com.cafeapp.domain.pointTransaction.entity.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
}
