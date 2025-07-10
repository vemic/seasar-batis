/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * カラムのメタデータを定義するアノテーションです。
 * <p>
 * このアノテーションを使用して、エンティティのフィールドと
 * データベースのカラムとのマッピング情報を定義します。
 * </p>
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/01/01
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SBColumnMeta {
    /**
     * データベースのカラム名を指定します。
     * 
     * @return カラム名
     */
    String name();
    
    /**
     * 主キーカラムかどうかを指定します。
     * 
     * @return 主キーの場合true、それ以外はfalse
     */
    boolean primaryKey() default false;
    
    /**
     * バージョンカラムかどうかを指定します。
     * <p>
     * バージョンカラムは楽観的排他制御に使用されます。
     * このカラムは更新時に自動的にインクリメントされ、
     * 更新条件として使用されます。
     * </p>
     * 
     * @return バージョンカラムの場合true、それ以外はfalse
     */
    boolean versionColumn() default false;
    
    /**
     * 最終更新日時カラムかどうかを指定します。
     * <p>
     * 最終更新日時カラムは楽観的排他制御に使用されます。
     * このカラムは更新時に現在時刻で自動的に更新され、
     * 更新条件として使用されます。
     * </p>
     * 
     * @return 最終更新日時カラムの場合true、それ以外はfalse
     */
    boolean lastModifiedColumn() default false;
}