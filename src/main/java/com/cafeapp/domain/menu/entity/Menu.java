package com.cafeapp.domain.menu.entity;

import com.cafeapp.domain.menu.exception.MenuException;
import com.cafeapp.global.entity.BaseEntity;
import com.cafeapp.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "menus")
public class Menu extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String menuName;

    @Column(nullable = false)
    private Long price;

    @Column(nullable = false)
    private int stock;

    @Enumerated(EnumType.STRING)
    private MenuStatus status;

    @Builder
    public Menu(String menuName, Long price, int stock, MenuStatus status) {
        this.menuName = menuName;
        this.price = price;
        this.stock = stock;
        this.status = status;
    }

    public void decreaseStock(int quantity) {
        if (this.stock < quantity) {
            throw new MenuException(ErrorCode.OUT_OF_STOCK);
        }
        this.stock -= quantity;
        if (this.stock == 0) {
            this.status = MenuStatus.SOLD_OUT;
        }
    }
}
