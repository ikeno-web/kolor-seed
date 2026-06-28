# 決定事項ログ

## 2026-06-28 Phase 0
### [決定] プロジェクト基本方針
- **内容**: 1色からMaterial 3パレット全体を生成するKotlin Multiplatformライブラリ
- **理由**: material-color-utilities (Google公式)はJava実装で重い。KMP対応の軽量代替がない
- **代替案**: material-color-utilities（Java/重い）、手動Color定義
- **影響範囲**: 全設計・実装
- **決定者**: ユーザー

### [決定] ターゲット
- **内容**: Compose / Compose Multiplatform 開発者
- **決定者**: ユーザー

### [決定] スコープ外
- **内容**: UIコンポーネント、Compose Theme統合、カラーピッカー
- **決定者**: ユーザー

## 2026-06-28 Phase 2
### [決定] Color 型は @JvmInline value class
- **内容**: `Color` を `@JvmInline value class(val argb: Int)` として定義
- **理由**: JVM でのボクシング回避。パレット生成で数百の Color インスタンスが生成されるため、ヒープ割り当てゼロが性能目標 (< 1ms) に必須
- **代替案**: `data class`（ヒープ割り当て発生）、`typealias Int`（型安全性なし）
- **影響範囲**: 全 API の型シグネチャ
- **決定者**: Designer Agent

### [決定] Hct は private constructor + ファクトリパターン
- **内容**: `Hct` クラスのコンストラクタを private にし、`from()` / `fromColor()` ファクトリのみ公開
- **理由**: HCT ソルバーによる ARGB 逆算を強制。不正な HCT 値からの直接生成を防止
- **代替案**: public constructor + validate（バリデーション漏れリスク）
- **影響範囲**: Hct 生成パス全体
- **決定者**: Designer Agent

### [決定] カスタムカラーは ExtendedCorePalette で分離
- **内容**: US-06 のカスタムカラーロールを `CorePalette` 本体ではなく `ExtendedCorePalette` ラッパーで実現
- **理由**: 基本 API (CorePalette → ColorScheme) の型シグネチャを単純に保つ。カスタムカラーは追加的なユースケースのため、複雑さを分離
- **代替案**: CorePalette に Map<String, TonalPalette> を直接持たせる（基本利用時にも Map を意識させる）
- **影響範囲**: US-06 関連の API 設計
- **決定者**: Designer Agent

### [決定] Compose 拡張に toM3ColorScheme() を追加
- **内容**: kolor-seed の ColorScheme → Compose Material 3 の ColorScheme への直接変換関数
- **理由**: ペルソナ A の主要ユースケース「MaterialTheme に直接渡す」を 1 関数呼び出しで実現
- **代替案**: 各色ロールを手動で ColorScheme() コンストラクタに渡す（ボイラープレート大）
- **影響範囲**: kolor-seed-compose モジュール。androidx.compose.material3 への依存が追加
- **決定者**: Designer Agent
