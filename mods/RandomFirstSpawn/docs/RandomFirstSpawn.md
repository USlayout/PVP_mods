# RandomFirstSpawn Guide

## Overview
- Server-side Forge 1.20.1 mod that teleports first-time players to a random location within a configurable radius around the shared world spawn and persists the coordinates for future deaths.

## Config (`config/randomspawn-common.toml`)
| Key | Description | Default | Range |
| --- | --- | --- | --- |
| `spawnRange` | Random radius from the shared spawn (blocks) | 4000 | 100–50000 |
| `minDistance` | Minimum straight-line distance to other players | 300 | 10–5000 |
| `maxTries` | Attempts before escalating search | 20 | 1–200 |
| `heightDiff` | Allowed elevation delta for the 7×7 flatness check | 4 | 1–20 |
| `debugLogs` | Print accept/reject reasons to the console | false | true / false |

```toml
["RandomSpawn Settings"]
    spawnRange = 4000
    minDistance = 300
    maxTries = 20
    heightDiff = 4
    debugLogs = false
```

- Toggle `debugLogs = true` when you need to see candidate coordinates and rejection reasons while tuning the values.

# 日本語説明

## 概要
- 初回ログインしたプレイヤーをワールド共有スポーンを中心とした半径範囲内でランダムにテレポートし、以降の死亡時も再利用できるよう座標を永続化する Forge 1.20.1 サーバー用 Mod。

## コンフィグ (`config/randomspawn-common.toml`)
| キー | 説明 | デフォルト | 範囲 |
| --- | --- | --- | --- |
| `spawnRange` | 共有スポーンからのランダム半径 (ブロック) | 4000 | 100–50000 |
| `minDistance` | 既存プレイヤーとの最低距離 | 300 | 10–5000 |
| `maxTries` | 1 回目の最大探索試行数 | 20 | 1–200 |
| `heightDiff` | 平坦判定で許容する高さ差 | 4 | 1–20 |
| `debugLogs` | 候補・却下理由を INFO で出力 | false | true / false |

```toml
["RandomSpawn Settings"]
    spawnRange = 4000
    minDistance = 300
    maxTries = 20
    heightDiff = 4
    debugLogs = false
```

- `debugLogs = true` にすると安全判定の詳細がコンソールに流れるため、設定調整や検証時に有効。
