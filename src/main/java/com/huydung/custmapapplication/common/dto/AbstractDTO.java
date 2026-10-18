package com.huydung.custmapapplication.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả phân trang dùng chung: trang hiện tại, số phần tử tối đa mỗi trang, tổng số phần tử và danh sách của trang đó.
 * page bắt đầu từ 1.
 */
@Getter
@Setter
@NoArgsConstructor
public class AbstractDTO<T> {
    private int page;
    private int maxPageItems;
    private long totalItem;
    private int totalPage;
    private List<T> resultList = new ArrayList<>();

    public static <T> AbstractDTO<T> of(List<T> resultList, int page, int maxPageItems, long totalItem) {
        AbstractDTO<T> dto = new AbstractDTO<>();
        dto.setResultList(resultList);
        dto.setPage(page);
        dto.setMaxPageItems(maxPageItems);
        dto.setTotalItem(totalItem);
        dto.setTotalPage(maxPageItems > 0 ? (int) Math.ceil((double) totalItem / maxPageItems) : 0);
        return dto;
    }
}
