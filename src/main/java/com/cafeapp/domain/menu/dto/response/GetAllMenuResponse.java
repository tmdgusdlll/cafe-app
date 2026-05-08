package com.cafeapp.domain.menu.dto.response;

import com.cafeapp.domain.menu.entity.Menu;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GetAllMenuResponse {

    private Long menuId;
    private String menuName;
    private Long price;
    private int stock;

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
