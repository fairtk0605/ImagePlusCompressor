# ZstdImage - ImageJ High-Efficiency Image Stack Compressor

ZstdImage は、**ImageJ** の画像スタック（動画やタイムラプスデータ）を、**Zstd (Zstandard)** および各種画像処理アルゴリズムを用いて超高効率に圧縮・保存するためのJavaライブラリおよびプラグインです。 

特に ZBGImage クラスによる**背景差分抽出＋輝度補正（Illumination Correction）**を用いた動画圧縮は、固定視点で撮影された顕微鏡画像や監視カメラなどのタイムラプスデータに対して圧倒的な圧縮率を発揮します。 

### 🌟 主な特徴

* **多彩な圧縮戦略 (CompressionStrategy):** 

  * NoLossStrategy: 画質劣化のない可逆圧縮（RawピクセルをそのままZstd圧縮）。
  * QuantizationStrategy: 色数（階調）を指定レベルに減色してデータ量を削減。
  * JpegStrategy: 各フレームを低容量なJPEG形式に変換した上でさらに圧縮。
* **革新的な背景差分シークエンス圧縮 (ZBGImage):** 

  * スタック全体の中央値（Median）から「基準背景」を自動生成。
  * フレームごとの輝度変化をアフィン変換係数（a ⋅ x + b）で自動補正。
  * 背景との微小なノイズ差分をゼロクリアし、残った有意な差分情報と背景情報のみをパックしてZstdで極限まで圧縮。
* **高速なシリアライズ:** 

  * 圧縮データおよびメタデータ（解像度・圧縮戦略）を **MessagePack (Jackson)** 形式でバイナリ保存。

### 🛠️ プロジェクト構成

ソースコードは以下のコンポーネントで構成されています。 

クラス / インターフェース 

役割 

**CompressionStrategy**
圧縮アルゴリズムのインターフェース。Jacksonによるポリモーフィズムに対応。
**ZstdImage**
基礎となる画像スタック圧縮クラス。各フレームを個別圧縮してZstdでラップ。
**ZBGImage**
ZstdImage を拡張し、背景差分と輝度補正アルゴリズムを実装した超高圧縮モデル。
**ZImpIO**
MessagePackを利用して、圧縮オブジェクトをファイルへ高速に入出力するIOクラス。
**ZImpSample**
実際にImageJ上で動作させて、各種戦略での圧縮・保存・復元をテストするサンプルプラグイン。

### 🚀 使い方 (Usage)

### 1. 基本的な圧縮と保存

ImageJの ImagePlus オブジェクトを、任意の戦略で圧縮してファイルに保存します。 

java

ImagePlus imp = IJ.getImage();

// 例: 64階調に量子化する戦略で圧縮
CompressionStrategy strategy = new QuantizationStrategy(64);
ZstdImage zImp = new ZstdImage(imp, strategy);

// ファイルへ保存 (MessagePack形式)
ZImpIO io = new ZImpIO();
io.write(zImp, "path/to/output.zimp");

コードは注意してご使用ください。

### 2. 背景差分を用いた高効率圧縮 (ZBGImage)

タイムラプス画像など、背景が固定されている動画に最適な圧縮方法です。 

java

ImagePlus imp = IJ.getImage();

// 可逆（NoLoss）の背景差分圧縮オブジェクトを生成
ZBGImage zBgImp = new ZBGImage(imp, new NoLossStrategy());

// 保存
ZImpIO io = new ZImpIO();
io.write(zBgImp, "path/to/video_compressed.zimp");

コードは注意してご使用ください。

### 3. データの読み込みと復元 (解凍)

保存された .zimp ファイルからオブジェクトを復元し、ImageJで再表示します。 

java

ZImpIO io = new ZImpIO();

// ファイルからデシリアライズ（自動で適切なStrategyが適用されます）
ZstdImage readZImp = io.read("path/to/output.zimp");

// ImageJのImagePlus形式にデコンプレスして表示
ImagePlus restoredImp = readZImp.restore();
restoredImp.show();

コードは注意してご使用ください。

### 📦 必要依存ライブラリ (Dependencies)

本プロジェクトの実行には、以下のライブラリ（jar）がクラスパスに通っている必要があります。 

* **ImageJ (ij.jar)**
* **zstd-jni** (com.github.luben:zstd-jni)
* **Jackson Databind** (com.fasterxml.jackson.core:jackson-databind)
* **Jackson Dataformat MessagePack** (org.msgpack:jackson-dataformat-msgpack)

### 📝 ライセンス (License)

[MIT License](LICENSE) (またはご自身の選んだライセンスを記載してください)
