/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.vemi.seasarbatis.core.config.SBOptimisticLockConfig.EntityLockConfig;
import jp.vemi.seasarbatis.core.config.SBOptimisticLockConfig.LockType;
import jp.vemi.seasarbatis.exception.SBException;

/**
 * 楽観的排他制御設定ファイルの読み込みを行うクラスです。
 * <p>
 * プロパティファイルから楽観的排他制御の設定を読み込み、
 * SBOptimisticLockConfigオブジェクトを構築します。
 * </p>
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/01/01
 */
public class SBOptimisticLockConfigLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(SBOptimisticLockConfigLoader.class);
    
    /**
     * デフォルトの設定ファイル名です。
     */
    public static final String DEFAULT_CONFIG_FILE = "seasarbatis-optimistic-lock.properties";
    
    /**
     * 設定のプレフィックスです。
     */
    private static final String CONFIG_PREFIX = "seasarbatis.optimistic-lock.";
    
    /**
     * デフォルトの設定ファイルから楽観的排他制御設定を読み込みます。
     * 
     * @return 楽観的排他制御設定
     */
    public static SBOptimisticLockConfig loadDefault() {
        return load(DEFAULT_CONFIG_FILE);
    }
    
    /**
     * 指定された設定ファイルから楽観的排他制御設定を読み込みます。
     * 
     * @param configFilePath 設定ファイルのパス
     * @return 楽観的排他制御設定
     */
    public static SBOptimisticLockConfig load(String configFilePath) {
        SBOptimisticLockConfig config = new SBOptimisticLockConfig();
        
        try (InputStream is = SBOptimisticLockConfigLoader.class.getClassLoader().getResourceAsStream(configFilePath)) {
            if (is == null) {
                logger.info("楽観的排他制御設定ファイルが見つかりません: {}. デフォルト設定を使用します。", configFilePath);
                return config;
            }
            
            Properties props = new Properties();
            props.load(is);
            
            // グローバル設定の読み込み
            loadGlobalConfig(config, props);
            
            // エンティティ固有設定の読み込み
            loadEntityConfigs(config, props);
            
            logger.info("楽観的排他制御設定ファイルを読み込みました: {}", configFilePath);
            
        } catch (IOException e) {
            throw new SBException("楽観的排他制御設定ファイルの読み込みに失敗しました: " + configFilePath, e);
        }
        
        return config;
    }
    
    /**
     * グローバル設定を読み込みます。
     * 
     * @param config 楽観的排他制御設定
     * @param props プロパティ
     */
    private static void loadGlobalConfig(SBOptimisticLockConfig config, Properties props) {
        // 楽観的排他制御の有効性
        String enabledStr = props.getProperty(CONFIG_PREFIX + "enabled", "true");
        config.setEnabled(Boolean.parseBoolean(enabledStr));
        
        // デフォルトの楽観的排他制御タイプ
        String defaultTypeStr = props.getProperty(CONFIG_PREFIX + "default-type", "NONE");
        try {
            LockType defaultType = LockType.valueOf(defaultTypeStr.toUpperCase());
            config.setDefaultLockType(defaultType);
        } catch (IllegalArgumentException e) {
            logger.warn("無効なデフォルト楽観的排他制御タイプです: {}. NONEを使用します。", defaultTypeStr);
            config.setDefaultLockType(LockType.NONE);
        }
    }
    
    /**
     * エンティティ固有設定を読み込みます。
     * 
     * @param config 楽観的排他制御設定
     * @param props プロパティ
     */
    private static void loadEntityConfigs(SBOptimisticLockConfig config, Properties props) {
        String entityPrefix = CONFIG_PREFIX + "entity.";
        
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith(entityPrefix)) {
                String remaining = key.substring(entityPrefix.length());
                String[] parts = remaining.split("\\.", 2);
                
                if (parts.length == 2) {
                    String className = parts[0];
                    String property = parts[1];
                    
                    try {
                        Class<?> entityClass = Class.forName(className);
                        
                        if ("type".equals(property)) {
                            String typeStr = props.getProperty(key);
                            LockType lockType = LockType.valueOf(typeStr.toUpperCase());
                            
                            String columnProperty = entityPrefix + className + ".column";
                            String columnName = props.getProperty(columnProperty);
                            
                            EntityLockConfig entityConfig = new EntityLockConfig(lockType, columnName);
                            config.addEntityConfig(entityClass, entityConfig);
                            
                            logger.debug("エンティティ楽観的排他制御設定を追加: {} -> {}, column: {}", 
                                       className, lockType, columnName);
                        }
                        
                    } catch (ClassNotFoundException e) {
                        logger.warn("エンティティクラスが見つかりません: {}", className);
                    } catch (IllegalArgumentException e) {
                        logger.warn("無効な楽観的排他制御タイプです: {} for {}", props.getProperty(key), className);
                    }
                }
            }
        }
    }
}