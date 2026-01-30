# ansible-plan-formatter

Ansible の JSON 出力をパースし、構造化 JSON または Markdown にフォーマットする CLI ツール。

## 概要

`ANSIBLE_STDOUT_CALLBACK=json` で得られる Ansible の実行結果 JSON を読み取り、構造化 JSON (デフォルト) または Markdown を出力します:

- ホストごとの stats (OK / Changed / Skipped / Failed / Unreachable)
- 変更されたタスクの一覧と Diff (prepared / before-after 形式に対応)
- 失敗したタスクとエラーメッセージ

終了コードは Ansible の慣例に合わせ、変更・失敗なしで `0`、変更または失敗ありで `2`、ツールエラーで `1` を返します。

## インストール

### GitHub Releases からバイナリを取得

```bash
curl -L https://github.com/boxp/ansible-plan-formatter/releases/latest/download/ansible-plan-formatter_linux_amd64.tar.gz \
  | tar xz
sudo mv ansible-plan-formatter /usr/local/bin/
```

### ソースからビルド (JVM)

```bash
clojure -T:build uber
java -jar target/ansible-plan-formatter-1.0.0-standalone.jar --help
```

## 使い方

```
Usage: ansible-plan-formatter [OPTIONS]

Options:
  -i, --input FILE      ファイルから読み込み (省略時 stdin)
  -n, --node NAME       ノード名ラベル
  -p, --playbook NAME   プレイブック名ラベル
  -f, --format FORMAT   出力形式 json|markdown (デフォルト: json)
  -h, --help            ヘルプ表示

Exit codes:
  0 - No changes or failures
  2 - Changes or failures detected
  1 - Tool error (invalid input, file not found, etc.)
```

### 例

```bash
# stdin から読み込み
ansible-playbook -i inventory site.yml --check --diff \
  -e 'ANSIBLE_STDOUT_CALLBACK=json' \
  | ansible-plan-formatter -n myhost -p site.yml

# ファイルから読み込み
ansible-plan-formatter -i result.json -n myhost -p site.yml

# Markdown 形式で出力 (後方互換)
ansible-plan-formatter -i result.json -n myhost -p site.yml --format markdown
```

### JSON 出力スキーマ

#### トップレベル

| フィールド | 型 | 説明 |
|-----------|-----|------|
| `version` | `integer` | スキーマバージョン (常に `1`) |
| `node` | `string` | ノード名 |
| `playbook` | `string` | プレイブック名 |
| `has_changes` | `boolean` | 変更または失敗があれば `true` |
| `stats` | `object<string, Stats>` | ホスト名をキーとした stats マップ |
| `changed_tasks` | `array<ChangedTask>` | 変更されたタスクの配列 |
| `failed_tasks` | `array<FailedTask>` | 失敗したタスクの配列 |

#### Stats

| フィールド | 型 | 説明 |
|-----------|-----|------|
| `ok` | `integer` | 成功タスク数 |
| `changed` | `integer` | 変更タスク数 |
| `skipped` | `integer` | スキップタスク数 |
| `failures` | `integer` | 失敗タスク数 |
| `unreachable` | `integer` | 到達不能数 |

#### ChangedTask

| フィールド | 型 | 説明 |
|-----------|-----|------|
| `name` | `string` | タスク名 |
| `module` | `string` | Ansible モジュール名 |
| `diff` | `Diff \| null` | diff 情報 (なければ `null`) |

#### Diff

`type` フィールドにより構造が異なります。

| type | フィールド | 型 | 説明 |
|------|-----------|-----|------|
| `prepared` | `content` | `string` | 整形済み diff テキスト |
| `before_after` | `before` | `string` | 変更前の内容 |
| `before_after` | `after` | `string` | 変更後の内容 |
| `before_after` | `before_header` | `string` | 変更前ファイルパス |
| `before_after` | `after_header` | `string` | 変更後ファイルパス |

#### FailedTask

| フィールド | 型 | 説明 |
|-----------|-----|------|
| `name` | `string` | タスク名 |
| `module` | `string` | Ansible モジュール名 |
| `msg` | `string` | エラーメッセージ |

### 出力例 (JSON, デフォルト)

変更がある場合:

```json
{
  "version": 1,
  "node": "myhost",
  "playbook": "site.yml",
  "has_changes": true,
  "stats": {
    "myhost": { "ok": 45, "changed": 3, "skipped": 12, "failures": 0, "unreachable": 0 }
  },
  "changed_tasks": [
    { "name": "Install nginx", "module": "apt",
      "diff": { "type": "prepared", "content": "- nginx (not installed)\n+ nginx 1.24.0" } },
    { "name": "Deploy config", "module": "template",
      "diff": { "type": "before_after", "before": "old", "after": "new",
                "before_header": "/etc/nginx.conf", "after_header": "/etc/nginx.conf" } },
    { "name": "Restart nginx", "module": "systemd", "diff": null }
  ],
  "failed_tasks": []
}
```

### 出力例 (Markdown, `--format markdown`)

```markdown
### myhost: site.yml

| Host | OK | Changed | Skipped | Failed | Unreachable |
|------|---:|--------:|--------:|-------:|------------:|
| myhost | 45 | 3 | 12 | 0 | 0 |

**3 to change**

<details>
<summary>Changed Tasks (3)</summary>

| # | Task | Module |
|--:|------|--------|
| 1 | Install nginx | apt |
| 2 | Deploy nginx config | template |
| 3 | Restart nginx | systemd |

</details>
```

## 開発

### 前提条件

- Java 21+
- [Clojure CLI](https://clojure.org/guides/install_clojure)

### コマンド

```bash
# テスト実行
make test

# リント
make lint

# フォーマットチェック
make format-check

# フォーマット自動修正
make format

# CI と同じチェックを一括実行 (format-check + lint + test)
make ci

# uberjar ビルド
make uber
```

### コーディング規約

- 関数は 10 行以内 (clj-kondo カスタムフックで警告)
- 行の長さは 100 文字以内
- `cljfmt` によるフォーマット統一
- TDD: テストを先に書き、実装を修正する

## ライセンス

MIT
