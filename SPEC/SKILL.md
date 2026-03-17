---
name: NVRegistryDesign
description: Requirements and Detailed Design for the NV Registry Android Application.
---

# NV Registry Android Application Design (v2)

## 1. 要件定義

### 1.1 目的
root権限を活用し `/dev/umts_router` 経由で端末のNVレジストリを読み書きするAndroidアプリ。

### 1.2 機能要件
| No | 要件 | ステータス |
|----|------|-----------|
| F1 | JSONファイル（SAF経由）の読み込みと一覧表示 | ✅ 実装済 |
| F2 | RegistryName・Payloadのリスト表示 | ✅ 実装済 |
| F3 | GET: `tail -f /dev/umts_router` で待受後、ATコマンド送信 | ✅ v2実装 |
| F4 | SET: ATコマンド実行後、自動でGETして結果確認 | ✅ v2実装 |
| F5 | SET後：変更前の値・JSONファイルの元値・変更後の値を表示 | ✅ v2実装 |
| F6 | 変更履歴の記録・閲覧 | ✅ v2実装 |
| F7 | 白/黒テーマ切替 | ✅ v2実装 |
| F8 | テキスト入力欄のフォント色を視認性の高い色に変更 | ✅ v2実装 |

### 1.3 UI要件
- メイン画面: ファイルを開くボタン・検索バー・テーマ切替ボタン・履歴ボタン・一覧
- 編集ダイアログ: RegistryName表示、GETボタン、コマンド出力（複数行）、JSONファイルの元値表示、変更前の値表示、SET後の取得値表示、SET入力欄、SETボタン
- 履歴画面: タイムスタンプ・RegistryName・JSONの値・変更前の値・設定した値・SET後にGETした値

## 2. 詳細設計

### 2.1 データモデル
#### RegistryEntry (data class)
- `Index`: Int
- `RegistryName`, `Size`, `Count`, `TypeName`, `Payload`: 基本フィールド

#### ChangeRecord (data class)
- `timestamp`: Long (epochMs)
- `registryName`: String
- `jsonPayload`: String — JSONファイルに設定されていた値
- `valueBeforeChange`: String — GET取得した変更前の値
- `newValue`: String — SETした値
- `postSetGetResult`: String — SET後にGETした返り値
- `success`: Boolean

### 2.2 コマンド仕様

#### GET コマンド（tail方式）
```
# SUセッション内で:
tail -f /dev/umts_router &
TAIL_PID=$!
sleep 0.3
echo 'AT+GOOGGETNV="RegistryName"\r' > /dev/umts_router
sleep 1
kill $TAIL_PID 2>/dev/null
```
※ tailでデバイスを読み待ち状態にしてからATコマンドを投入することで確実にレスポンスを取得する。

#### SET コマンド + 自動GET
```
echo 'AT+GOOGSETNV="RegistryName",0,"NewValue"\r' > /dev/umts_router & cat /dev/umts_router
# その後、自動でGETも実行して結果確認
```

### 2.3 変更履歴
- ファイル: `{filesDir}/change_history.json`
- 形式: `List<ChangeRecord>` のJSON配列
- 最大保持件数: 制限なし（デバイスストレージ依存）

### 2.4 テーマ
- `SharedPreferences` で `"theme"` キーに `"dark"` / `"light"` を保存
- Activity再起動なしで背景色・テキスト色を動的切替

### 2.5 テキスト入力欄の視認性
- EditTextの `textColor` をテーマ依存の適切な色に設定
- ダークテーマ: テキスト白 (#FFFFFF), 背景 #2C2C2C
- ライトテーマ: テキスト黒 (#000000), 背景 #FFFFFF

## 3. ファイル構成（v2以降）

```
android/app/src/main/
├── AndroidManifest.xml        ← HistoryActivity追加
├── java/com/example/nvregistry/
│   ├── MainActivity.kt        ← テーマ切替・履歴ボタン追加
│   ├── RegistryEditDialog.kt  ← SET後自動GET・値比較表示
│   ├── HistoryActivity.kt     ← 新規: 変更履歴画面
│   ├── adapter/
│   │   ├── RegistryAdapter.kt
│   │   └── HistoryAdapter.kt  ← 新規
│   ├── model/
│   │   ├── RegistryEntry.kt
│   │   └── ChangeRecord.kt    ← 新規
│   └── util/
│       ├── ShellUtils.kt      ← tail方式GETに変更
│       └── ChangeHistoryManager.kt ← 新規
└── res/layout/
    ├── activity_main.xml       ← テーマ切替・履歴ボタン追加
    ├── activity_history.xml    ← 新規
    ├── dialog_edit_registry.xml ← 値比較表示追加
    ├── registry_item.xml
    └── history_item.xml        ← 新規
```
