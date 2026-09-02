# ImageJ Zstandard Image Compression Plugin

ImageJ/Fiji上で動作する、Zstandard (Zstd) と MessagePack を用いた高効率な8bitモノクロ画像（またはスタック動画）の圧縮・保存用プラグインライブラリです。

## 特徴

- **ZstdImage**: ImageJのImagePlus（スタック）をZstandardで可逆圧縮。
- **ZQImage**: 指定した階調数（Colors）に量子化した上でZstd圧縮をかける非可逆圧縮。
- **ZQBGImage**: 動画スタックから時間方向の基準背景を合成（メディアン等）し、フレームごとのコントラスト補正（ゲイン・オフセット）を計算した上で、背景差分データのみを圧縮保持する超高効率圧縮。
- **MessagePackシリアライズ**: Jacksonデータバインディング経由で `.zimp` 拡張子のコンパクトなバイナリファイルとして保存・復元が可能。

## 必要要件

- Java 8 以上
- ImageJ / Fiji
- 依存ライブラリ（詳細は下記参照）

## 依存関係 (Dependencies)

本プロジェクトをビルド、または動作させるには以下のライブラリが必要です。

### Maven を利用する場合 (`pom.xml`)

`pom.xml` の `<dependencies>` 構成に以下を追加してください。

```xml
<dependencies>
    <!-- ImageJ API -->
    <dependency>
        <groupId>net.imagej</groupId>
        <artifactId>ij</artifactId>
        <version>1.54g</version> <!-- 任意のバージョン -->
        <scope>provided</scope>
    </dependency>

    <!-- Zstandard Java bindings -->
    <dependency>
        <groupId>com.github.luben</groupId>
        <artifactId>zstd-jni</artifactId>
        <version>1.5.7-8</version>
    </dependency>

    <!-- Jackson Databind -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.17.2</version> <!-- 任意のバージョン -->
    </dependency>

    <!-- Jackson Dataformat MessagePack -->
    <dependency>
        <groupId>org.msgpack</groupId>
        <artifactId>jackson-dataformat-msgpack</artifactId>
        <version>0.9.12</version>
    </dependency>
</dependencies>
```

### Fiji / ImageJ に手動導入する場合

ビルドしたJARファイルと同時に、以下の依存JARファイルを ImageJ の `plugins` もしくは `jars` フォルダに配置する必要があります。

1. [zstd-jni](https://mvnrepository.com/artifact/com.github.luben/zstd-jni)
2. [jackson-databind](https://mvnrepository.com) / `jackson-core` / `jackson-annotations`
3. [jackson-dataformat-msgpack](https://mvnrepository.com/artifact/org.msgpack/jackson-dataformat-msgpack)

## 使い方 (サンプルコード)

プラグインとして実行する場合の最小構成コード（`ZImpSample.java`）の処理フローです。ImageJで画像を開いた状態で実行します。

```java
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;

public class ZImpSample implements PlugIn {    
    public void run(String arg) {
        // 1. 開いている画像を取得
        ImagePlus imp = IJ.getImage();

        // 2. 各種圧縮インスタンスの生成
        ZstdImage zImp0 = new ZstdImage(imp);       // 標準Zstd圧縮
        ZQImage   zImp1 = new ZQImage(imp, 64);     // 64階調量子化 + Zstd
        ZQBGImage zImp2 = new ZQBGImage(imp);       // 背景差分アルゴリズム圧縮

        // 3. ファイル保存・読み込み・復元・表示のテスト
        saveAndreadAndShowZimp("F0", zImp0);
        saveAndreadAndShowZimp("F1", zImp1);
        saveAndreadAndShowZimp("F2", zImp2);
    }
    // ... (詳細はソースコードを参照)
}
```

## ライセンス

[MIT License](LICENSE) (※利用するライセンスに合わせて変更してください)
