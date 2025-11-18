package com.early_express.inventory_service.global.common.utils;

import com.early_express.inventory_service.global.common.dto.PageInfo;
import com.early_express.inventory_service.global.common.dto.PageInfo.SortInfo;
import com.early_express.inventory_service.global.presentation.dto.PageResponse;
import com.early_express.inventory_service.global.presentation.exception.GlobalErrorCode;
import com.early_express.inventory_service.global.presentation.exception.GlobalException;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PageUtils {

    private PageUtils() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    /**
     * Spring Data Page를 PageResponse로 변경
     */
    public static <T> PageResponse<T> toPageResponse(Page<T> page) {
        validatePage(page);

        PageInfo pageInfo = createPageInfo(page);
        return PageResponse.of(page.getContent(), pageInfo);
    }

    /**
     * Spring Data Page를 매퍼 함수를 사용하여 PageResponse로 변환
     * 엔티티 -> DTO 변환 시 사용
     */
    public static <T, R> PageResponse<R> toPageResponse(Page<T> page, Function<T, R> mapper) {
        validatePage(page);
        validateMapper(mapper);

        List<R> mappedContent = page.getContent().stream()
                .map(mapper)
                .collect(Collectors.toList());

        PageInfo pageInfo = createPageInfo(page);
        return PageResponse.of(mappedContent, pageInfo);
    }

    private static <T> PageInfo createPageInfo(Page<T> page) {
        // 정렬 정보 추출
        List<SortInfo> sortInfos = page.getSort().stream()
                .map(order -> SortInfo.of(
                        order.getProperty(),
                        order.isAscending() ? SortInfo.Direction.ASC : SortInfo.Direction.DESC,
                        order.isIgnoreCase()
                ))
                .collect(Collectors.toList());

        // 정렬 정보 포함하여 PageInfo 생성
        return PageInfo.of(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumberOfElements(),
                sortInfos
        );
    }

    private static void validatePage(Page<?> page) {
        if (page == null) {
            throw new PageUtilException(GlobalErrorCode.INVALID_INPUT_VALUE, "페이지 객체는 null일 수 없습니다.");
        }
    }

    private static void validateMapper(Function<?, ?> mapper) {
        if (mapper == null) {
            throw new PageUtilException(GlobalErrorCode.INVALID_INPUT_VALUE, "매퍼 함수는 null일 수 없습니다.");
        }
    }

    /**
     * PageUtils 전용 예외 클래스
     */
    public static class PageUtilException extends GlobalException {
        public PageUtilException(GlobalErrorCode errorCode, String message) {
            super(errorCode, message);
        }
    }
}