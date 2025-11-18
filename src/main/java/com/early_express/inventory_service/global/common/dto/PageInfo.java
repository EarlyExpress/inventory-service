package com.early_express.inventory_service.global.common.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class PageInfo {
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final int numberOfElements;
    private final boolean first;
    private final boolean last;
    private final boolean hasNext;
    private final boolean hasPrevious;
    private final boolean empty;
    private final List<SortInfo> sort;

    @Builder
    private PageInfo(int page, int size, long totalElements,
                     int totalPages, int numberOfElements, List<SortInfo> sort) {
        validateParameters(page, size, totalElements, totalPages);

        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.numberOfElements = numberOfElements;
        this.first = (page == 0);
        this.last = (page == totalPages - 1) || (totalPages == 0);
        this.hasNext = page < totalPages - 1;
        this.hasPrevious = page > 0;
        this.empty = numberOfElements == 0;
        this.sort = sort != null ? new ArrayList<>(sort) : new ArrayList<>();
    }

    // 정렬 정보 없이 생성 (기존 호환성 유지)
    public static PageInfo of(int page, int size, long totalElements,
                              int totalPages, int numberOfElements) {
        return PageInfo.builder()
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .numberOfElements(numberOfElements)
                .build();
    }

    // 정렬 정보 포함하여 생성
    public static PageInfo of(int page, int size, long totalElements,
                              int totalPages, int numberOfElements, List<SortInfo> sort) {
        return PageInfo.builder()
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .numberOfElements(numberOfElements)
                .sort(sort)
                .build();
    }

    private void validateParameters(int page, int size, long totalElements, int totalPages) {
        if (page < 0) {
            throw new IllegalArgumentException("page는 0보다 크거나 같아야 됩니다.");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size는 0보다 커야합니다.");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements는 0보다 크거나 같아야 합니다.");
        }
        if (totalPages < 0) {
            throw new IllegalArgumentException("totalPages는 0보다 크거나 같아야 합니다.");
        }
    }

    /**
     * 정렬 정보를 담는 내부 클래스
     */
    @Getter
    @Builder
    public static class SortInfo {
        private final String property;
        private final Direction direction;
        private final boolean ignoreCase;

        public enum Direction {
            ASC, DESC
        }

        public static SortInfo of(String property, Direction direction) {
            return SortInfo.builder()
                    .property(property)
                    .direction(direction)
                    .ignoreCase(false)
                    .build();
        }

        public static SortInfo of(String property, Direction direction, boolean ignoreCase) {
            return SortInfo.builder()
                    .property(property)
                    .direction(direction)
                    .ignoreCase(ignoreCase)
                    .build();
        }
    }
}