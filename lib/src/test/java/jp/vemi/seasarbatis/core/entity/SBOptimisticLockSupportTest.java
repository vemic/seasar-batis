/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import jp.vemi.seasarbatis.core.config.SBOptimisticLockConfig;
import jp.vemi.seasarbatis.core.config.SBOptimisticLockConfig.LockType;
import jp.vemi.seasarbatis.core.entity.SBOptimisticLockSupport.OptimisticLockInfo;
import jp.vemi.seasarbatis.core.entity.SBOptimisticLockSupport.VersionColumnInfo;
import jp.vemi.seasarbatis.core.entity.SBOptimisticLockSupport.LastModifiedColumnInfo;
import jp.vemi.seasarbatis.test.entity.OptimisticLockTestUser;

/**
 * 楽観的排他制御サポートクラスのテストです。
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/01/01
 */
class SBOptimisticLockSupportTest {

    @Test
    void testGetVersionColumnInfo() {
        // Given
        OptimisticLockTestUser user = new OptimisticLockTestUser(1L, "Test User", "test@example.com");
        user.setVersion(5L);
        
        // When
        Optional<VersionColumnInfo> versionInfo = SBOptimisticLockSupport.getVersionColumnInfo(user);
        
        // Then
        assertTrue(versionInfo.isPresent());
        assertEquals("version", versionInfo.get().getColumnName());
        assertEquals(5L, versionInfo.get().getValue());
        assertNotNull(versionInfo.get().getField());
    }

    @Test
    void testGetLastModifiedColumnInfo() {
        // Given
        OptimisticLockTestUser user = new OptimisticLockTestUser(1L, "Test User", "test@example.com");
        LocalDateTime now = LocalDateTime.now();
        user.setUpdatedAt(now);
        
        // When
        Optional<LastModifiedColumnInfo> lastModifiedInfo = SBOptimisticLockSupport.getLastModifiedColumnInfo(user);
        
        // Then
        assertTrue(lastModifiedInfo.isPresent());
        assertEquals("updated_at", lastModifiedInfo.get().getColumnName());
        assertEquals(now, lastModifiedInfo.get().getValue());
        assertNotNull(lastModifiedInfo.get().getField());
    }

    @Test
    void testGetOptimisticLockInfo_VersionType() {
        // Given
        OptimisticLockTestUser user = new OptimisticLockTestUser(1L, "Test User", "test@example.com");
        user.setVersion(3L);
        
        SBOptimisticLockConfig config = new SBOptimisticLockConfig()
                .setEnabled(true)
                .setDefaultLockType(LockType.VERSION);
        
        // When
        OptimisticLockInfo lockInfo = SBOptimisticLockSupport.getOptimisticLockInfo(user, config);
        
        // Then
        assertEquals(LockType.VERSION, lockInfo.getLockType());
        assertEquals("version", lockInfo.getColumnName());
        assertEquals(3L, lockInfo.getCurrentValue());
        assertTrue(lockInfo.isEnabled());
    }

    @Test
    void testGetOptimisticLockInfo_LastModifiedType() {
        // Given
        OptimisticLockTestUser user = new OptimisticLockTestUser(1L, "Test User", "test@example.com");
        LocalDateTime timestamp = LocalDateTime.now();
        user.setUpdatedAt(timestamp);
        
        SBOptimisticLockConfig config = new SBOptimisticLockConfig()
                .setEnabled(true)
                .setDefaultLockType(LockType.LAST_MODIFIED);
        
        // When
        OptimisticLockInfo lockInfo = SBOptimisticLockSupport.getOptimisticLockInfo(user, config);
        
        // Then
        assertEquals(LockType.LAST_MODIFIED, lockInfo.getLockType());
        assertEquals("updated_at", lockInfo.getColumnName());
        assertEquals(timestamp, lockInfo.getCurrentValue());
        assertTrue(lockInfo.isEnabled());
    }

    @Test
    void testGetOptimisticLockInfo_Disabled() {
        // Given
        OptimisticLockTestUser user = new OptimisticLockTestUser(1L, "Test User", "test@example.com");
        
        SBOptimisticLockConfig config = new SBOptimisticLockConfig()
                .setEnabled(false);
        
        // When
        OptimisticLockInfo lockInfo = SBOptimisticLockSupport.getOptimisticLockInfo(user, config);
        
        // Then
        assertEquals(LockType.NONE, lockInfo.getLockType());
        assertFalse(lockInfo.isEnabled());
    }

    @Test
    void testUpdateOptimisticLockValue_Version() {
        // Given
        OptimisticLockTestUser user = new OptimisticLockTestUser(1L, "Test User", "test@example.com");
        user.setVersion(2L);
        
        SBOptimisticLockConfig config = new SBOptimisticLockConfig()
                .setEnabled(true)
                .setDefaultLockType(LockType.VERSION);
        
        OptimisticLockInfo lockInfo = SBOptimisticLockSupport.getOptimisticLockInfo(user, config);
        
        // When
        Object newValue = SBOptimisticLockSupport.updateOptimisticLockValue(user, lockInfo);
        
        // Then
        assertEquals(3L, newValue);
        assertEquals(3L, user.getVersion());
    }

    @Test
    void testUpdateOptimisticLockValue_LastModified() {
        // Given
        OptimisticLockTestUser user = new OptimisticLockTestUser(1L, "Test User", "test@example.com");
        LocalDateTime oldTimestamp = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        user.setUpdatedAt(oldTimestamp);
        
        SBOptimisticLockConfig config = new SBOptimisticLockConfig()
                .setEnabled(true)
                .setDefaultLockType(LockType.LAST_MODIFIED);
        
        OptimisticLockInfo lockInfo = SBOptimisticLockSupport.getOptimisticLockInfo(user, config);
        
        // When
        Object newValue = SBOptimisticLockSupport.updateOptimisticLockValue(user, lockInfo);
        
        // Then
        assertNotNull(newValue);
        assertTrue(newValue instanceof LocalDateTime);
        assertTrue(((LocalDateTime) newValue).isAfter(oldTimestamp));
        assertEquals(newValue, user.getUpdatedAt());
    }

    @Test
    void testBuildOptimisticLockCondition() {
        // Given
        OptimisticLockTestUser user = new OptimisticLockTestUser(1L, "Test User", "test@example.com");
        user.setVersion(4L);
        
        SBOptimisticLockConfig config = new SBOptimisticLockConfig()
                .setEnabled(true)
                .setDefaultLockType(LockType.VERSION);
        
        OptimisticLockInfo lockInfo = SBOptimisticLockSupport.getOptimisticLockInfo(user, config);
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        
        // When
        String condition = SBOptimisticLockSupport.buildOptimisticLockCondition(lockInfo, params);
        
        // Then
        assertEquals(" AND version = /*optimisticLockValue*/null", condition);
        assertEquals(4L, params.get("optimisticLockValue"));
    }
}