# NV Registry Viewer for Android

<p align="center">
  <img src="docs/icon.png" alt="NV Registry Viewer" width="120" />
</p>

Android application to display, read, and write NV (Non-Volatile) Registry values on Pixel 8/9/10 series devices using AT commands via `/dev/umts_router`.

---

## ⚠️ Prerequisites / 利用前提

| Item | Detail |
|------|--------|
| **Target Device** | **Pixel 8, 9, 10 series only** (uses `/dev/umts_router`) |
| **Input File** | JSON file obtained via **`nv_dump`** command |
| **Root Access** | **Required** — commands are executed via `su` |
| **DM Port** | Apps that occupy the DM port (e.g., **NSG**, QXDM, etc.) **must be closed** before using GET/SET — otherwise commands will not respond |

---

## 📱 Features / 機能

- Load NV Registry JSON files using the **Storage Access Framework** (no fixed path required)
- Display all registry entries with **TypeName**, **HEX**, and **Decimal** values parsed per `Size × Count`
- **GET** current device values using `tail -f /dev/umts_router` (reliable multi-line capture)
- **SET** per-index values individually — `AT+GOOGSETNV="NAME",{index},"bytes"`
- Automatic **GET after SET** to verify the written value
- **Change history** — records JSON value / before value / set value / post-SET GET result
- **White / Dark theme** toggle
- **English / Japanese** language toggle
- **Debug log** toggle (Logcat output for troubleshooting)

---

## 🏗️ Architecture / アーキテクチャ

```
android/app/src/main/
├── AndroidManifest.xml
└── java/com/example/nvregistry/
    ├── MainActivity.kt          # File selection, search, theme/language toggle
    ├── RegistryEditDialog.kt    # GET/SET dialog with per-index editing
    ├── HistoryActivity.kt       # Change history view
    ├── adapter/
    │   ├── RegistryAdapter.kt   # Main list (HEX/Dec display)
    │   ├── NvValueAdapter.kt    # Per-index row in dialog
    │   └── HistoryAdapter.kt    # History list
    ├── model/
    │   ├── RegistryEntry.kt     # JSON data model
    │   └── ChangeRecord.kt      # History record model
    └── util/
        ├── ShellUtils.kt        # Root shell command execution
        ├── PayloadParser.kt     # Payload → HEX/Dec conversion
        ├── ChangeHistoryManager.kt  # History persistence
        └── DebugConfig.kt       # Debug log toggle
```

---

## 🔧 AT Command Format / ATコマンド仕様

| Operation | Command |
|-----------|---------|
| GET | `echo 'AT+GOOGGETNV="<Name>"\r' > /dev/umts_router` |
| SET | `echo 'AT+GOOGSETNV="<Name>",<index>,"<bytes>"\r' > /dev/umts_router` |

Bytes are in **little-endian** format, comma-separated hex (e.g., `01,00` for `u16` value `1`).

---

## 🐛 Debug Log / デバッグログ

Press the **"DBG OFF → ON"** button in the toolbar to enable verbose logging.  
Filter Logcat with tags: `ShellUtils`, `RegistryEditDialog`

> ⚠️ Disable debug logging in production to avoid performance overhead.

---

## ⚠️ Disclaimer / 免責事項

Writing incorrect NV values can cause **device malfunction or boot failure**.  
Use this tool at your own risk on devices where you understand the implications.

NVの値を誤って書き込むと**端末の動作不良やブート不能**になる可能性があります。  
十分に理解した上で、自己責任でご利用ください。

---

## 📄 License

MIT License

---

# NV Registry Viewer — 日本語説明

Pixel 8/9/10シリーズ向けのNVレジストリ閲覧・書き込みAndroidアプリです。  
`/dev/umts_router` 経由でATコマンドを使用してNV値を読み書きします。

### 利用前提

| 項目 | 内容 |
|------|------|
| **対応機種** | **Pixel 8 / 9 / 10シリーズのみ** |
| **入力ファイル** | **`nv_dump`コマンドで取得したJSONファイル** |
| **root権限** | **必須** — `su`コマンドでシェル実行します |
| **DMポート占有アプリ** | **NSG・QXDMなどDMポートを使用するアプリは終了してください** |

### 機能
- SAFによる任意のJSONファイル読み込み
- TypeName・Size・Countに基づくHEX/Decimal表示
- `tail -f`方式による確実なGET（複数行対応）
- インデックス指定のSET（`AT+GOOGSETNV="名前",{index},"bytes"`）
- SET後の自動GETで結果確認
- 変更履歴の記録・閲覧
- 白/黒テーマ切替
- 日本語/英語切替
- デバッグログON/OFF
