このリポジトリは更新を放棄しました。
最新版は hogandiff3 リポジトリをご覧ください。
---

# 方眼Diff

行や列のギャップも自動で検出できるExcelファイル比較ツールです。

## 方眼Diffの特徴

特長：

- 誰でも簡単かつ直感的に利用できます。
- 行や列の挿入や削除によるギャップを自動で検出できます。
- 通常のデスクトップアプリケーションとしての利用に加えて、TortoiseGitなどのバージョン管理ツールと組み合わせて利用することもできます。

出来ること：

- Excelシート同士を比較し、内容の異なるセルを検出できます。
- セルの内容が数式の場合、数式文字列／計算結果の値のどちらで比較を行うかを指定できます。
- 行や列の挿入や削除によるギャップを自動で検出できます。
- 2つのExcelファイルを指定することで、それらに含まれる名前の似たシート同士を自動でマッチングし、複数のシートを一度に比較することができます。
- .xls/.xlsx/.xlsm形式のExcelファイルのワークシートに対応しています。異なる形式のExcelファイル同士を比較することもできます。

出来ないこと：

- 比較できるのはセル内容の文字列のみです。それ以外の一切、例えばフォント、セル背景色、セルコメント、オブジェクトなどを比較することはできません。
- .xlsb形式のExcelファイルには対応していません。また、ワークシート以外のシート（グラフシートやダイアログシートなど）には対応していません。
- 方眼Diffはシンプルで使いやすいことを最重視して設計されています。そのため、比較条件やレポート形式を細かく指定することはできません。
- Excelは高機能なアプリケーションであり、世界中で様々な用途や方法で利用されています。方眼Diffは可能な限り正しく機能することを目指していますが、対応できない場合があります。

詳細は[Webサイト](https://hogandiff.hotchpotch.xyz/)をご覧ください。

## インストール方法

方眼Diffはお使いのPCのレジストリを更新しません。ダウンロードしたzipファイルを解凍してお好きな場所に配置するだけで利用できます。

1. 「hogandiff-x.x.x.zip」ファイルをダウンロードしてお好きな場所に保存します。
2. ダウンロードしたzipファイルを解凍ツールを利用して解凍します。下記のようなフォルダ階層に展開されます。
3. 解凍された「方眼Diff-x.x.x」フォルダを丸ごとお好きな場所に配置してお使いください。

```
方眼Diff-x.x.x\
  ├─ jre-min\
  │   └─ (多数のファイル群)
  ├─ lib\
  │   └─ (多数のファイル群)
  ├─ 方眼Diff.exe
  ├─ 方眼Diff.exe.vmoptions
  ├─ LICENSE
  └─ README.md
```

「方眼Diff.exe」が方眼Diffの本体です。これをダブルクリックして利用します。「方眼Diff.exe」のショートカットをデスクトップなどに作成しておくと便利でしょう。

ただし、「方眼Diff-x.x.x」フォルダの内容はそのままでご利用ください。「方眼Diff.exe」だけを別の場所に取り出すと正常に動作しませんのでご注意ください。

## アンインストール方法

「方眼Diff.x.x.x」フォルダを丸ごと削除してください。

## 使い方

### 通常のデスクトップアプリケーションとして利用する場合

「方眼Diff.exe」をダブルクリックしてアプリケーションを起動します。後はアプリケーションの画面項目に従って操作してください。比較対象のExcelファイル2つを指定して「実行」ボタンを押下すると、比較が行われます。

### TortoiseGitなどのバージョン管理ツールと組み合わせて利用する場合

「方眼Diff.exe」は比較対象のExcelファイルのパス2つをパラメータに与えることにより、コマンドラインから起動することができます。

```
> 方眼Diff.exe Excelファイルパス1 Excelファイルパス2
```

各種オプションを指定することもできます。  
詳細は[Webサイト](https://hogandiff.hotchpotch.xyz/)をご覧ください。

## 提供ライセンス

「方眼Diff.exe」は nmby の著作物であり、MITライセンスのもと提供しています。  
詳細はこのファイル（README.md）と同じフォルダに格納されている LICENSE ファイルをご参照ください。

このファイル（README.md）と同じフォルダに格納されている「jre-min」、「lib」両サブフォルダには以下の第三者著作物が含まれており、それらのライセンス許諾に従って nmby が再頒布しています。

- AdoptOpenJDKバイナリ
    - GPL v2 with Classpath Exception ライセンスにより提供されています。
    - 詳細は[AdoptOpenJDKのWebサイト](https://adoptopenjdk.net/)および「方眼Diff-x.x.x\jre-min\legal\」フォルダ内の関連ファイルをご参照ください。
- OpenJFX
    - GPL v2 with Classpath Exception ライセンスにより提供されています。
    - 詳細は[OpenJFXのWebサイト](https://openjfx.io/)および「方眼Diff-x.x.x\jre-min\legal\」、「方眼Diff-x.x.x\lib\javafx-13\legal\」両フォルダ内の関連ファイルをご参照ください。
- Apache POI
    - Apache License, Version 2.0 ライセンスにより提供されています。
    - 詳細は[Apache POIのWebサイト](https://poi.apache.org/)および「方眼Diff-x.x.x\lib\poi-4.1.0\」フォルダ内の関連ファイルをご参照ください。

あなたが方眼Diffを利用する場合は以上の全てのライセンスに従う必要があります。

## 連絡先

e-mail  : nmby@hotchpotch.xyz  
website : https://xyz.hotchpotch.hogandiff/

