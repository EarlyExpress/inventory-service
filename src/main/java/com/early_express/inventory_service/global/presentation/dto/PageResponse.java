package com.early_express.inventory_service.global.presentation.dto;

import com.early_express.inventory_service.global.common.dto.PageInfo;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public class PageResponse<T> {
    private final List<T> content;
    private final PageInfo pageInfo;

    private PageResponse(List<T> content, PageInfo pageInfo) {
        validatePageInfo(pageInfo);
        this.content = content != null ? content : Collections.emptyList();
        this.pageInfo = pageInfo;
    }

    public static <T> PageResponse<T> of(List<T> content, PageInfo pageInfo) {
        return new PageResponse<>(content, pageInfo);
    }

    private void validatePageInfo(PageInfo pageInfo) {
        if (pageInfo == null) {
            throw new IllegalArgumentException("페이지 정보는 null이 될 수 없습니다.");
        }
    }
}
