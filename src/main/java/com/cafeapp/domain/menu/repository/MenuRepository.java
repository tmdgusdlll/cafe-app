package com.cafeapp.domain.menu.repository;

import com.cafeapp.domain.menu.entity.Menu;
import com.cafeapp.domain.menu.entity.MenuStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    List<Menu> findAllByStatus(MenuStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Menu m WHERE m.id = :id")
    Optional<Menu> findByIdWithPessimisticLock(@Param("id") Long id);
}
