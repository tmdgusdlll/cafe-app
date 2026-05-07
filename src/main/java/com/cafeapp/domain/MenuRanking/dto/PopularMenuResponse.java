package com.cafeapp.domain.MenuRanking.dto;

import com.cafeapp.domain.menu.entity.Menu;
import lombok.Getter;

@Getter
public class PopularMenuResponse {

    private final int rank;
    private final Long menuId;
    private final String menuName;
    private final Long orderCount;

    public PopularMenuResponse(int rank, Long menuId, String menuName, Long orderCount) {
        this.rank = rank;
        this.menuId = menuId;
        this.menuName = menuName;
        this.orderCount = orderCount;
    }
}
