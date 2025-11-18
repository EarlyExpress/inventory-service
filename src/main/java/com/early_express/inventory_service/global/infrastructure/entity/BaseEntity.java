package com.early_express.inventory_service.global.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 기본 엔티티 - 감사(Audit) 정보 관리
 * - createdAt: 생성 시간
 * - createdBy: 생성자 ID
 * - updatedAt: 수정 시간
 * - updatedBy: 수정자 ID
 * - deletedAt: 삭제 시간
 * - deletedBy: 삭제자 ID
 * - isDeleted: 삭제 여부 (Soft Delete)
 *
 * 사용 예시:
 * <pre>
 * {@code
 *  @Entity
 *  @Table(name = "example")
 *  public class ExampleEntity extends BaseEntity {
 *      @Id
 *      @Column(name = "id", length = 36)
 *      private String id;
 *
 *      private String name;
 *      // 생성일, 생성자, 수정일, 수정자, 삭제 정보는 자동 관리
 *  }
 * }
 * </pre>
 */
@Getter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /**
     * 생성 시간 (서버 시간 기준)
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    /**
     * 생성자 ID
     */
    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 36)
    private String createdBy;

    /**
     * 수정 시간 (서버 시간 기준)
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 수정자 ID
     */
    @LastModifiedBy
    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    /**
     * 삭제 시간
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 삭제자 ID
     */
    @Column(name = "deleted_by", length = 36)
    private String deletedBy;

    /**
     * 삭제 여부 (Soft Delete)
     */
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    /**
     * Soft Delete 처리
     * @param deletedBy 삭제자 ID
     */
    public void delete(String deletedBy) {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }

    /**
     * Soft Delete 복구
     */
    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
    }
}