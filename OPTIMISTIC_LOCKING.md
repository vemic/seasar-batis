# SeasarBatis 楽観的排他制御機能

SeasarBatisでは、バージョンカラムまたは最終更新日時に基づく楽観的排他制御を提供します。

## 基本概念

楽観的排他制御は、データベースレコードの更新時に、他のトランザクションによる競合更新を検出する仕組みです。SeasarBatisでは以下の2つの方式をサポートしています：

- **バージョンカラム方式**: 数値型のバージョンカラムを使用し、更新時に自動的にインクリメントします
- **最終更新日時方式**: 日時型のカラムを使用し、更新時に現在時刻を設定します

## エンティティの設定

### バージョンカラム方式

```java
@SBTableMeta(name = "users")
public class User {
    @SBColumnMeta(name = "id", primaryKey = true)
    private Long id;
    
    @SBColumnMeta(name = "name")
    private String name;
    
    @SBColumnMeta(name = "version", versionColumn = true)
    private Long version;
    
    // getters/setters...
}
```

### 最終更新日時方式

```java
@SBTableMeta(name = "orders")
public class Order {
    @SBColumnMeta(name = "id", primaryKey = true)
    private Long id;
    
    @SBColumnMeta(name = "amount")
    private BigDecimal amount;
    
    @SBColumnMeta(name = "updated_at", lastModifiedColumn = true)
    private LocalDateTime updatedAt;
    
    // getters/setters...
}
```

### 両方を使用する場合

```java
@SBTableMeta(name = "products")
public class Product {
    @SBColumnMeta(name = "id", primaryKey = true)
    private Long id;
    
    @SBColumnMeta(name = "name")
    private String name;
    
    @SBColumnMeta(name = "version", versionColumn = true)
    private Long version;
    
    @SBColumnMeta(name = "updated_at", lastModifiedColumn = true)
    private LocalDateTime updatedAt;
    
    // getters/setters...
}
```

## 設定ファイルによる管理

`src/main/resources/seasarbatis-optimistic-lock.properties`ファイルで楽観的排他制御を設定できます：

```properties
# グローバル設定
seasarbatis.optimistic-lock.enabled=true
seasarbatis.optimistic-lock.default-type=NONE

# エンティティ固有設定
seasarbatis.optimistic-lock.entity.com.example.User.type=VERSION
seasarbatis.optimistic-lock.entity.com.example.User.column=version

seasarbatis.optimistic-lock.entity.com.example.Order.type=LAST_MODIFIED
seasarbatis.optimistic-lock.entity.com.example.Order.column=updated_at
```

### 設定パラメータ

- `enabled`: 楽観的排他制御の有効性（true/false）
- `default-type`: デフォルトの楽観的排他制御タイプ（VERSION/LAST_MODIFIED/NONE）
- `entity.[クラス名].type`: エンティティ固有の楽観的排他制御タイプ
- `entity.[クラス名].column`: 使用するカラム名（省略時はアノテーション自動検出）

## プログラムによる設定

```java
// 楽観的排他制御設定を作成
SBOptimisticLockConfig config = new SBOptimisticLockConfig()
    .setEnabled(true)
    .setDefaultLockType(LockType.VERSION);

// エンティティ固有設定を追加
config.addEntityConfig(User.class, 
    new EntityLockConfig(LockType.VERSION, null)); // アノテーション自動検出

config.addEntityConfig(Order.class,
    new EntityLockConfig(LockType.LAST_MODIFIED, "updated_at"));

// SBJdbcManagerに設定を適用
SBJdbcManager jdbcManager = new SBJdbcManager(sqlSessionFactory, config);
```

## 基本的な使用方法

楽観的排他制御が設定されている場合、通常の`update`メソッドで自動的に楽観的排他制御が適用されます：

```java
// ユーザーを取得
User user = jdbcManager.findByPk(User.class, 1L).getSingleResult();
Long originalVersion = user.getVersion(); // 例: 5

// ユーザー情報を更新
user.setName("Updated Name");
User updatedUser = jdbcManager.update(user);

// バージョンが自動的にインクリメントされる
assert updatedUser.getVersion().equals(originalVersion + 1); // 6
```

## 楽観的排他制御エラーの処理

他のトランザクションによってレコードが更新された場合、`SBOptimisticLockException`がスローされます：

```java
try {
    User user = jdbcManager.findByPk(User.class, 1L).getSingleResult();
    user.setName("Updated Name");
    jdbcManager.update(user);
} catch (SBOptimisticLockException e) {
    // 楽観的排他制御エラーの処理
    System.err.println("レコードが他のトランザクションによって更新されています: " + e.getMessage());
    
    // 最新のデータを再取得して再試行
    User latestUser = jdbcManager.findByPk(User.class, 1L).getSingleResult();
    latestUser.setName("Updated Name");
    jdbcManager.update(latestUser);
}
```

## 設定の読み込み

設定ファイルからの自動読み込み：

```java
// デフォルト設定ファイル（seasarbatis-optimistic-lock.properties）を読み込み
SBOptimisticLockConfig config = SBOptimisticLockConfigLoader.loadDefault();

// 独自の設定ファイルを読み込み
SBOptimisticLockConfig config = SBOptimisticLockConfigLoader.load("my-optimistic-lock.properties");

SBJdbcManager jdbcManager = new SBJdbcManager(sqlSessionFactory, config);
```

## 注意事項

1. **バージョンカラムの型**: 数値型（Long、Integer等）である必要があります
2. **最終更新日時カラムの型**: Date、LocalDateTime、その他のTemporal実装である必要があります
3. **初期値の設定**: 新規レコード作成時は、バージョンカラムに1、最終更新日時カラムに現在時刻が設定されます
4. **複数カラムの優先順位**: バージョンカラムと最終更新日時カラムの両方が存在する場合、設定に基づいて一方が使用されます
5. **パフォーマンス**: 楽観的排他制御により、WHERE句に追加条件が含まれるため、適切なインデックスの設定を推奨します

## サンプルコード

完全なサンプルコードについては、`lib/src/test/java/jp/vemi/seasarbatis/test/entity/OptimisticLockTestUser.java`および関連するテストクラスを参照してください。