# ansible-plan-formatter

Ansible の JSON 出力をパースし、GitHub PR コメント向けの Markdown にフォーマットする CLI ツール。

## 概要

`ANSIBLE_STDOUT_CALLBACK=json` で得られる Ansible の実行結果 JSON を読み取り、以下を含む Markdown を出力します:

- ホストごとの stats テーブル (OK / Changed / Skipped / Failed / Unreachable)
- 変更されたタスクの一覧
- Diff の詳細 (prepared / before-after 形式に対応)
- 失敗したタスクとエラーメッセージ

終了コードは Ansible の慣例に合わせ、変更なしで `0`、変更ありで `2`、エラーで `1` を返します。

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
java -jar target/ansible-plan-formatter-0.1.0-standalone.jar --help
```

## 使い方

```
Usage: ansible-plan-formatter [OPTIONS]

Options:
  -i, --input FILE    ファイルから読み込み (省略時 stdin)
  -n, --node NAME     ノード名ラベル
  -p, --playbook NAME プレイブック名ラベル
  -h, --help          ヘルプ表示

Exit codes:
  0 - No changes
  2 - Changes detected
  1 - Error
```

### 例

```bash
# stdin から読み込み
ansible-playbook -i inventory site.yml --check --diff \
  -e 'ANSIBLE_STDOUT_CALLBACK=json' \
  | ansible-plan-formatter -n myhost -p site.yml

# ファイルから読み込み
ansible-plan-formatter -i result.json -n myhost -p site.yml
```

### 出力例

変更がある場合:

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
