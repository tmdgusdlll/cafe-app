package com.cafeapp.domain.menu.dto.response;

import com.cafeapp.domain.menu.entity.Menu;
import lombok.Getter;

@Getter
public class GetAllMenuResponse {

    private final Long menuId;
    private final String menuName;
    private final Long price;
    private final int stock;

    private GetAllMenuResponse(Long menuId, String menuName, Long price, int stock) {
        this.menuId = menuId;
        this.menuName = menuName;
        this.price = price;
        this.stock = stock;
    }

    public static GetAllMenuResponse from(Menu menu) {
        return new GetAllMenuResponse(
                menu.getId(),
                menu.getMenuName(),
                menu.getPrice(),
                menu.getStock()
        );
    }
}
