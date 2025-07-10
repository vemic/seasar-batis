/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.jdbc;

import static jp.vemi.seasarbatis.core.entity.SBEntityOperations.getEntityParams;
import static jp.vemi.seasarbatis.core.entity.SBEntityOperations.getPrimaryKeyValues;
import static jp.vemi.seasarbatis.core.entity.SBEntityOperations.getTableName;
import static jp.vemi.seasarbatis.core.sql.CommandType.DELETE;
import static jp.vemi.seasarbatis.core.sql.CommandType.INSERT;
import static jp.vemi.seasarbatis.core.sql.CommandType.SELECT;
import static jp.vemi.seasarbatis.core.sql.CommandType.UPDATE;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.vemi.seasarbatis.core.builder.SBDeleteBuilder;
import jp.vemi.seasarbatis.core.builder.SBSelectBuilder;
import jp.vemi.seasarbatis.core.builder.SBUpdateBuilder;
import jp.vemi.seasarbatis.core.config.SBOptimisticLockConfig;
import jp.vemi.seasarbatis.core.criteria.ComplexWhere;
import jp.vemi.seasarbatis.core.criteria.SimpleWhere;
import jp.vemi.seasarbatis.core.entity.SBOptimisticLockSupport;
import jp.vemi.seasarbatis.core.entity.SBOptimisticLockSupport.OptimisticLockInfo;
import jp.vemi.seasarbatis.core.query.SBSelect;
import jp.vemi.seasarbatis.core.sql.executor.SBQueryExecutor;
import jp.vemi.seasarbatis.core.transaction.SBTransactionCallback;
import jp.vemi.seasarbatis.core.transaction.SBTransactionManager;
import jp.vemi.seasarbatis.core.transaction.SBTransactionManager.PropagationType;
import jp.vemi.seasarbatis.core.transaction.SBTransactionOperation;
import jp.vemi.seasarbatis.exception.SBException;
import jp.vemi.seasarbatis.exception.SBIllegalStateException;
import jp.vemi.seasarbatis.exception.SBOptimisticLockException;

/**
 * JDBC操作を簡素化するマネージャークラス。 Seasar2のJdbcManagerに似た操作性を提供します。
 * <p>
 * 本クラスは、MyBatisのSqlSessionを使用してデータベース操作を提供します。
 * </p>
 *
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/01/01
 */
public class SBJdbcManager {
    private static final Logger logger = LoggerFactory.getLogger(SBJdbcManager.class);

    private final SqlSessionFactory sqlSessionFactory;
    private final SBTransactionManager txManager;
    private final SBQueryExecutor queryExecutor;
    private final SBOptimisticLockConfig optimisticLockConfig;

    /**
     * {@link SBJdbcManager}を構築します。
     *
     * @param sqlSessionFactory {@link SqlSessionFactory}
     */
    public SBJdbcManager(SqlSessionFactory sqlSessionFactory) {
        this(sqlSessionFactory, new SBOptimisticLockConfig());
    }

    /**
     * {@link SBJdbcManager}を構築します。
     *
     * @param sqlSessionFactory {@link SqlSessionFactory}
     * @param optimisticLockConfig 楽観的排他制御設定
     */
    public SBJdbcManager(SqlSessionFactory sqlSessionFactory, SBOptimisticLockConfig optimisticLockConfig) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.optimisticLockConfig = optimisticLockConfig;
        this.txManager = new SBTransactionManager(sqlSessionFactory);
        this.queryExecutor = new SBQueryExecutor(sqlSessionFactory.getConfiguration(), txManager.getTransactionOperation());
    }

    /**
     * {@link SBJdbcManager}を構築します。
     *
     * @param dataSource {@link DataSource}
     */
    public SBJdbcManager(DataSource dataSource) {
        this(createSqlSessionFactory(dataSource), new SBOptimisticLockConfig());
    }

    /**
     * {@link SBJdbcManager}を構築します。
     *
     * @param dataSource {@link DataSource}
     * @param optimisticLockConfig 楽観的排他制御設定
     */
    public SBJdbcManager(DataSource dataSource, SBOptimisticLockConfig optimisticLockConfig) {
        this(createSqlSessionFactory(dataSource), optimisticLockConfig);
    }

    /**
     * SqlSessionFactoryを生成します。
     *
     * @param dataSource {@link DataSource}
     * @return {@link SqlSessionFactory}
     */
    private static SqlSessionFactory createSqlSessionFactory(DataSource dataSource) {
        try (Reader reader = Resources.getResourceAsReader("mybatis-config.xml")) {
            SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(reader);
            factory.getConfiguration().setEnvironment(new org.apache.ibatis.mapping.Environment("development",
                    new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory(), dataSource));
            logger.info("MyBatis設定を読み込みました");
            return factory;
        } catch (IOException e) {
            logger.error("MyBatis設定の読み込みに失敗しました: {}", e.getMessage(), e);
            throw new SBException("MyBatis設定の読み込みに失敗しました", e);
        }
    }

    /**
     * {@link SqlSessionFactory}を取得します。
     *
     * @return {@link SqlSessionFactory}
     */
    public SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }

    // SQL実行
    /**
     * SQL文に基づいて検索を実行します。
     *
     * @param <T>        戻り値の要素型
     * @param sql        SQL文
     * @param params     パラメータ
     * @param resultType 結果のマッピング先クラス
     * @return 検索結果のリスト
     */
    public <T> SBSelect<T> selectBySql(String sql, Map<String, Object> params, Class<T> resultType) {
        return this.<T>select().from(resultType).withSql(sql).withParams(params);
    }

    /**
     * SQLファイルに基づいて検索を実行します。
     *
     * @param <T>        戻り値の要素型
     * @param sqlFile    SQLファイルのパス
     * @param params     パラメータ
     * @param resultType 結果のマッピング先クラス
     * @return 検索結果のリスト
     */
    public <T> SBSelect<T> selectBySqlFile(String sqlFile, Map<String, Object> params, Class<T> resultType) {
        return this.<T>select().from(resultType).withSqlFile(sqlFile).withParams(params);
    }

    /**
     * INSERT文を実行します。
     *
     * @param sql    SQL文
     * @param params パラメータ
     * @return 実行結果
     */
    public int insert(String sql, Map<String, Object> params) {
        return queryExecutor.execute(sql, params, INSERT);
    }

    /**
     * SQLファイルからINSERT文を実行します。
     *
     * @param sqlFile SQLファイルパス
     * @param params  パラメータ
     * @return 実行結果
     */
    public int insertBySqlFile(String sqlFile, Map<String, Object> params) {
        return queryExecutor.executeFile(sqlFile, params, INSERT);
    }

    /**
     * UPDATE文を実行します。
     *
     * @param sql    SQL文
     * @param params パラメータ
     * @return 更新された行数
     */
    public int update(String sql, Map<String, Object> params) {
        return queryExecutor.execute(sql, params, UPDATE);
    }

    /**
     * SQLファイルからUPDATE文を実行します。
     *
     * @param sqlFile SQLファイルパス
     * @param params  パラメータ
     * @return 更新された行数
     */
    public int updateBySqlFile(String sqlFile, Map<String, Object> params) {
        return queryExecutor.executeFile(sqlFile, params, UPDATE);
    }

    /**
     * DELETE文を実行します。
     *
     * @param sql    SQL文
     * @param params パラメータ
     * @return 削除された行数
     */
    public int delete(String sql, Map<String, Object> params) {
        return queryExecutor.execute(sql, params, DELETE);
    }

    /**
     * SQLファイルからDELETE文を実行します。
     *
     * @param sqlFile SQLファイルパス
     * @param params  パラメータ
     * @return 削除された行数
     */
    public int deleteBySqlFile(String sqlFile, Map<String, Object> params) {
        return queryExecutor.executeFile(sqlFile, params, DELETE);
    }

    // ---------- エンティティ操作 ----------
    /**
     * 主キーに基づいてエンティティを検索します。
     *
     * @param <T>    エンティティの型
     * @param entity 検索対象のPK情報を含むエンティティ
     * @return 検索されたエンティティ、存在しない場合はnull
     * @throws IllegalArgumentException 指定された主キーの数が不正な場合
     */
    @SuppressWarnings("unchecked")
    public <T> SBSelect<T> findByPk(T entity) {
        return this.<T>select().from((Class<T>) entity.getClass()).byPrimaryKey(getPrimaryKeyValues(entity));
    }

    /**
     * 主キーに基づいてエンティティを検索します。（例外をスローしない）
     *
     * @param <T>    エンティティの型
     * @param entity 検索対象のPK情報を含むエンティティ
     * @return 検索されたエンティティ、存在しない場合はnull
     * @throws IllegalArgumentException 指定された主キーの数が不正な場合
     */
    @SuppressWarnings("unchecked")
    public <T> SBSelect<T> findByPkNoException(T entity) {
        return this.<T>select().from((Class<T>) entity.getClass()).byPrimaryKey(getPrimaryKeyValues(entity))
                .suppressException();
    }

    /**
     * エンティティの全件を検索します。
     *
     * @param <T>         エンティティの型
     * @param entityClass エンティティのクラス
     * @return エンティティのリスト
     */
    public <T> List<T> findAll(Class<T> entityClass) {
        return this.<T>select().from(entityClass).getResultList();
    }

    /**
     * エンティティを新規登録します。
     *
     * @param <T>    エンティティの型
     * @param entity 登録するエンティティ
     * @return 登録されたエンティティ
     */
    public <T> T insert(T entity) {
        return insert(entity, false);
    }

    /**
     * エンティティを新規登録します。
     *
     * @param <T>                      エンティティの型
     * @param entity                   登録するエンティティ
     * @param isIndependentTransaction 独立したトランザクションで実行するかどうか
     * @return 登録されたエンティティ
     */
    public <T> T insert(T entity, boolean isIndependentTransaction) {
        return executeWithTransaction(isIndependentTransaction, () -> {
            String tableName = getTableName(entity.getClass());
            Map<String, Object> params = getEntityParams(entity);

            StringBuilder sql = new StringBuilder("INSERT INTO " + tableName + " (");
            StringBuilder values = new StringBuilder(") VALUES (");

            params.forEach((column, value) -> {
                sql.append(column).append(", ");
                values.append("/*").append(column).append("*/null, ");
            });

            sql.setLength(sql.length() - 2);
            values.setLength(values.length() - 2);
            sql.append(values).append(")");

            queryExecutor.execute(sql.toString(), params, INSERT);

            @SuppressWarnings("unchecked")
            SBSelect<T> newSelect = this.<T>select().from((Class<T>) entity.getClass())
                    .byPrimaryKey(getPrimaryKeyValues(entity));
            return newSelect.getSingleResult();
        });
    }

    /**
     * 主キーに基づいてエンティティを更新します。
     *
     * @param <T>    エンティティの型
     * @param entity 更新するエンティティ
     * @return 更新されたエンティティ
     * @throws IllegalArgumentException 主キーが設定されていない場合
     */
    public <T> T update(T entity) {
        return update(entity, false);
    }

    /**
     * 主キーに基づいてエンティティを更新します。
     *
     * @param <T>                      エンティティの型
     * @param entity                   更新するエンティティ
     * @param isIndependentTransaction 独立したトランザクションで実行するかどうか
     * @return 更新されたエンティティ
     * @throws IllegalArgumentException 主キーが設定されていない場合
     * @throws SBOptimisticLockException 楽観的排他制御に失敗した場合
     */
    @SuppressWarnings("unchecked")
    public <T> T update(T entity, boolean isIndependentTransaction) {
        return executeWithTransaction(isIndependentTransaction, () -> {
            String tableName = getTableName(entity.getClass());
            Map<String, Object> pkValues = getPrimaryKeyValues(entity);

            if (pkValues.isEmpty()) {
                throw new SBIllegalStateException("主キーが設定されていません");
            }

            // 楽観的排他制御の情報を取得
            OptimisticLockInfo lockInfo = SBOptimisticLockSupport.getOptimisticLockInfo(entity, optimisticLockConfig);
            
            Map<String, Object> params = getEntityParams(entity);
            // 主キーを params から削除
            pkValues.keySet().forEach(params::remove);

            StringBuilder sql = new StringBuilder("UPDATE " + tableName + " SET ");

            // 楽観的排他制御用カラムの値を更新（バージョン番号のインクリメントや更新日時の設定）
            if (lockInfo.isEnabled()) {
                Object newOptimisticLockValue = SBOptimisticLockSupport.updateOptimisticLockValue(entity, lockInfo);
                if (newOptimisticLockValue != null) {
                    params.put(lockInfo.getColumnName(), newOptimisticLockValue);
                }
            }

            // 主キー以外のカラムを更新対象とする
            params.forEach((column, value) -> {
                if (!pkValues.containsKey(column)) {
                    sql.append(column).append(" = /*").append(column).append("*/null, ");
                }
            });

            sql.setLength(sql.length() - 2);
            sql.append(" WHERE ");

            // 複数の主キーでWHERE句を構築
            int pkCount = 0;
            for (Map.Entry<String, Object> pk : pkValues.entrySet()) {
                if (pkCount++ > 0)
                    sql.append(" AND ");
                sql.append(pk.getKey()).append(" = /*pk").append(pkCount).append("*/0");
                params.put("pk" + pkCount, pk.getValue());
            }

            // 楽観的排他制御のWHERE句条件を追加
            if (lockInfo.isEnabled()) {
                String optimisticLockCondition = SBOptimisticLockSupport.buildOptimisticLockCondition(lockInfo, params);
                sql.append(optimisticLockCondition);
            }

            int updatedRows = queryExecutor.execute(sql.toString(), params, UPDATE);
            if (updatedRows == 0) {
                if (lockInfo.isEnabled()) {
                    throw new SBOptimisticLockException(
                            "楽観的排他制御エラー: レコードが他のトランザクションによって更新されています。", 
                            entity, 
                            lockInfo.getColumnName());
                } else {
                    throw new SBOptimisticLockException("更新対象のレコードが見つかりませんでした。他のトランザクションによって更新された可能性があります。", entity,
                            pkValues.keySet().toArray(new String[0]));
                }
            }

            List<T> newEntity = queryExecutor
                    .executeSelect(
                            "SELECT * FROM " + tableName + " WHERE "
                                    + pkValues.entrySet().stream().map(e -> e.getKey() + " = '" + e.getValue() + "'")
                                            .collect(Collectors.joining(" AND ")),
                            params, (Class<T>) entity.getClass());
            return newEntity.isEmpty() ? null : newEntity.get(0);
        });
    }

    /**
     * エンティティを1件削除します。
     * 
     * <p>
     * エンティティの主キー情報に基づいて、該当するレコードを削除します。 主キーが設定されていない場合は例外がスローされます。
     * </p>
     *
     * @param <T>    エンティティの型
     * @param entity 削除対象のエンティティ
     * @return 削除された件数
     * @throws SBIllegalStateException 主キーが設定されていない場合
     */
    public <T> int delete(T entity) {
        return delete(entity, false);
    }

    /**
     * エンティティを1件削除します。
     * 
     * <p>
     * エンティティの主キー情報に基づいて、該当するレコードを削除します。 主キーが設定されていない場合は例外がスローされます。
     * </p>
     *
     * @param <T>                      エンティティの型
     * @param entity                   削除対象のエンティティ
     * @param isIndependentTransaction 独立したトランザクションで実行するかどうか
     * @return 削除された件数
     * @throws SBIllegalStateException 主キーが設定されていない場合
     */
    public <T> int delete(T entity, boolean isIndependentTransaction) {
        return executeWithTransaction(isIndependentTransaction, () -> {
            Map<String, Object> pkValues = getPrimaryKeyValues(entity);
            if (pkValues.isEmpty()) {
                throw new SBIllegalStateException("主キーが設定されていません: " + entity.getClass().getName());
            }

            String tableName = getTableName(entity.getClass());
            StringBuilder sql = new StringBuilder("DELETE FROM " + tableName + " WHERE ");

            Map<String, Object> params = new HashMap<>();
            int pkCount = 0;
            for (Map.Entry<String, Object> pk : pkValues.entrySet()) {
                if (pkCount++ > 0) {
                    sql.append(" AND ");
                }
                sql.append(pk.getKey()).append(" = /*pk").append(pkCount).append("*/0");
                params.put("pk" + pkCount, pk.getValue());
            }

            return queryExecutor.execute(sql.toString(), params, DELETE);
        });
    }

    /**
     * エンティティを登録または更新します。 主キーが設定されており、レコードが存在する場合は更新を行います。 それ以外の場合は新規登録を行います。
     *
     * @param <T>    エンティティの型
     * @param entity 登録または更新するエンティティ
     * @return 処理されたエンティティ
     */
    public <T> T insertOrUpdate(T entity) {
        return insertOrUpdate(entity, false);
    }

    /**
     * エンティティを登録または更新します。 主キーが設定されており、レコードが存在する場合は更新を行います。 それ以外の場合は新規登録を行います。
     *
     * @param <T>                      エンティティの型
     * @param entity                   登録または更新するエンティティ
     * @param isIndependentTransaction 独立したトランザクションで実行するかどうか
     * @return 処理されたエンティティ
     */
    public <T> T insertOrUpdate(T entity, boolean isIndependentTransaction) {
        return executeWithTransaction(isIndependentTransaction, () -> {
            Map<String, Object> pkValues = getPrimaryKeyValues(entity);

            // 主キーが未設定の場合はINSERT
            if (pkValues.values().stream().allMatch(value -> value == null)) {
                logger.debug("主キーが未設定のため、INSERTを実行します");
                return insert(entity, isIndependentTransaction);
            }

            // 主キーで検索して存在確認
            Class<?> entityClass = entity.getClass();
            String tableName = getTableName(entityClass);
            StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM " + tableName + " WHERE ");

            Map<String, Object> params = new HashMap<>();
            int pkCount = 0;
            for (Map.Entry<String, Object> pk : pkValues.entrySet()) {
                if (pkCount++ > 0) {
                    sql.append(" AND ");
                }
                sql.append(pk.getKey()).append(" = /*pk").append(pkCount).append("*/0");
                params.put("pk" + pkCount, pk.getValue());
            }

            List<Map<String, Object>> result = queryExecutor.execute(sql.toString(), params, SELECT);
            long count = ((Number) result.get(0).values().iterator().next()).longValue();

            if (count > 0) {
                logger.debug("レコードが存在するため、UPDATEを実行します");
                return update(entity, isIndependentTransaction);
            } else {
                logger.debug("レコードが存在しないため、INSERTを実行します");
                return insert(entity, isIndependentTransaction);
            }
        });
    }

    // ---------- Fluent API ----------
    /**
     * 型安全な検索クエリを開始します
     *
     * @param <T> エンティティの型
     * @return 型安全な検索ビルダー
     */
    public <T> SBSelect<T> select() {
        return new SBSelect<>(sqlSessionFactory, queryExecutor, txManager.getTransactionOperation());
    }

    /**
     * 型安全な検索クエリを特定のSQL文を使用して開始します
     *
     * @param <T> エンティティの型
     * @param sql SQL文
     * @return 型安全な検索ビルダー
     */
    @SuppressWarnings("unchecked")
    public <T> SBSelect<T> select(String sql) {
        return (SBSelect<T>) select().withSql(sql);
    }

    /**
     * エンティティに対するSelect操作を開始します。
     * 
     * @param <T>         エンティティの型
     * @param entityClass エンティティのクラス
     * @return SelectビルダーのFromインスタンス
     */
    public <T> SBSelectBuilder<T> from(Class<T> entityClass) {
        return new SBSelectBuilder<T>(this, entityClass);
    }

    /**
     * UPDATE文の構築を開始します。
     * 
     * @param <T>         エンティティの型
     * @param entityClass 更新対象のエンティティクラス
     * @return UpdateBuilderインスタンス
     */
    public <T> SBUpdateBuilder<T> update(Class<T> entityClass) {
        return new SBUpdateBuilder<>(this, entityClass);
    }

    /**
     * DELETE文の構築を開始します。
     * 
     * @param <T>         エンティティの型
     * @param entityClass 削除対象のエンティティクラス
     * @return DeleteBuilderインスタンス
     */
    public <T> SBDeleteBuilder<T> delete(Class<T> entityClass) {
        return new SBDeleteBuilder<>(this, entityClass);
    }

    /**
     * 条件式を生成するSimpleWhereインスタンスを作成します。
     * 
     * @return 新しいSimpleWhereインスタンス
     */
    public SimpleWhere where() {
        return new SimpleWhere();
    }

    /**
     * 複合条件式を生成するComplexWhereインスタンスを作成します。
     * 
     * @return 新しいComplexWhereインスタンス
     */
    public ComplexWhere complexWhere() {
        return new ComplexWhere();
    }

    // ---------- トランザクション管理 ----------
    /**
     * トランザクション処理を実行します。
     *
     * @param callback トランザクションコールバック
     */
    public void transaction(SBTransactionCallback callback) {
        transaction(callback, false);
    }

    /**
     * トランザクション処理を実行します。
     *
     * @param callback                 トランザクションコールバック
     * @param isIndependentTransaction 独立したトランザクションで実行するかどうか
     */
    public void transaction(SBTransactionCallback callback, boolean isIndependentTransaction) {
        txManager.execute(isIndependentTransaction ? PropagationType.REQUIRES_NEW : PropagationType.REQUIRED,
                () -> {
                    callback.execute(this);
                    return null;
                });
    }

    // ---------- Getter ----------
    public SBTransactionManager getTransactionManager() {
        return this.txManager;
    }

    /**
     * 楽観的排他制御設定を取得します。
     * 
     * @return 楽観的排他制御設定
     */
    public SBOptimisticLockConfig getOptimisticLockConfig() {
        return this.optimisticLockConfig;
    }

    // ---------- Utility ----------
    private <T> T executeWithTransaction(boolean isIndependentTransaction, Callable<T> operation) {
        return txManager.execute(isIndependentTransaction ? PropagationType.REQUIRES_NEW : PropagationType.REQUIRED,
                operation);
    }

}