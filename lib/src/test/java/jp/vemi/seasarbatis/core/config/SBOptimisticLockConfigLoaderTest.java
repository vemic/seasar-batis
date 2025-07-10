/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import jp.vemi.seasarbatis.core.config.SBOptimisticLockConfig.LockType;

/**
 * 楽観的排他制御設定ローダーのテストです。
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/01/01
 */
class SBOptimisticLockConfigLoaderTest {

    @Test
    void testLoadDefault() {
        // When
        SBOptimisticLockConfig config = SBOptimisticLockConfigLoader.loadDefault();
        
        // Then
        assertNotNull(config);
        assertTrue(config.isEnabled());
        assertEquals(LockType.NONE, config.getDefaultLockType());
    }

    @Test
    void testLoadNonExistentFile() {
        // When
        SBOptimisticLockConfig config = SBOptimisticLockConfigLoader.load("non-existent-file.properties");
        
        // Then
        assertNotNull(config);
        assertTrue(config.isEnabled()); // デフォルト値
        assertEquals(LockType.NONE, config.getDefaultLockType()); // デフォルト値
    }
}