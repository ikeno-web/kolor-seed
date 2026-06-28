# 禁止事項・制約リスト

## 技術的制約
- [必須] Kotlin Multiplatform (JVM/JS/Native)
- [必須] 純粋関数のみ（副作用なし）
- [必須] Android依存なし（android.graphics.Color等は禁止）
- [禁止] 外部依存ゼロ（kotlin-stdlib以外）
- [禁止] mutableなグローバル状態

## 設計的制約
- [必須] HCT色空間の正確な実装
- [必須] 全関数がスレッドセーフ
- [必須] inline関数の積極活用（パフォーマンス）
- [禁止] リフレクション使用

## OSSとしての制約
- [必須] README.md は英語
- [必須] Apache 2.0 License（Kotlin ecosystem慣例）
- [必須] テストカバレッジ 95%以上
- [必須] Maven Central publish 可能な状態
- [必須] KDoc 全public API
