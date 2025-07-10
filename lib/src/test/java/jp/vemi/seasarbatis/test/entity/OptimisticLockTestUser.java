/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.test.entity;

import jp.vemi.seasarbatis.core.meta.SBColumnMeta;
import jp.vemi.seasarbatis.core.meta.SBTableMeta;

import java.time.LocalDateTime;

/**
 * 楽観的排他制御テスト用のユーザーエンティティです。
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/01/01
 */
@SBTableMeta(name = "users")
public class OptimisticLockTestUser {
    
    @SBColumnMeta(name = "id", primaryKey = true)
    private Long id;
    
    @SBColumnMeta(name = "name")
    private String name;
    
    @SBColumnMeta(name = "email")
    private String email;
    
    @SBColumnMeta(name = "version", versionColumn = true)
    private Long version;
    
    @SBColumnMeta(name = "updated_at", lastModifiedColumn = true)
    private LocalDateTime updatedAt;
    
    /**
     * デフォルトコンストラクタです。
     */
    public OptimisticLockTestUser() {
    }
    
    /**
     * コンストラクタです。
     * 
     * @param id ID
     * @param name 名前
     * @param email メールアドレス
     */
    public OptimisticLockTestUser(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.version = 1L;
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public String toString() {
        return "OptimisticLockTestUser{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", version=" + version +
                ", updatedAt=" + updatedAt +
                '}';
    }
}