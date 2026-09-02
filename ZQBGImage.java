import ij.*;
import ij.plugin.ZProjector;
import ij.process.*;

import java.util.Arrays;
import com.github.luben.zstd.Zstd;

public class ZQBGImage extends ZQImage {
    public int bgColors, diffColors;
    public byte[] zBgImg;
    public byte[] zDiffImg;

    public ZQBGImage() {super();}
    public ZQBGImage(ImagePlus imp){compress(imp,16,16,96,32,ZProjector.MEDIAN_METHOD);}
    public void compress(ImagePlus srcImp, int motionTh, int noiseTh, int bgColors, int diffColors,int projectionMethod) {
        // 0. Data Init
        byte[] rawData = setImagePlus(srcImp);
        this.bgColors = Math.min(256, Math.max(2, bgColors));
        this.diffColors = Math.min(256, Math.max(2, diffColors));

        // 1. ZProjectorによる基準背景合成
        ZProjector zp = new ZProjector(srcImp);
        zp.setMethod(projectionMethod);
        zp.doProjection();
        byte[] refData = (byte[]) zp.getProjection().getProcessor().getPixels();

        // 2. 各フレームのコントラスト調整パラメータ(ゲインa・オフセットb)事前計算
        double[] aArr = new double[this.size * this.width * this.height];
        double[] bArr = new double[this.size * this.width * this.height];
        for (int f = 1; f <= this.size; f++) 
            calcCorrParam(rawData, refData, aArr, bArr, f, motionTh);

        // 3. 引き算 & ノイズカット
        byte[] cData = new byte[rawData.length];
        for (int i = 0; i < rawData.length; i++){
            double  corr = aArr[i] * (rawData[i] & 0xff) + bArr[i];
            int corrData = Math.min(255, Math.max(0, (int) Math.round(corr)));
            int   bgData = refData[i%(this.width*this.height)]; 
            int qDiff = corrData - bgData;
            if(Math.abs(qDiff) < noiseTh) qDiff = 0;
            cData[i] = (byte)(qDiff);
        } 

        // 3. compress
        this.zBgImg   = Zstd.compress(getQuantized(refData,bgColors), 3);
        this.zDiffImg = Zstd.compress(getQuantized(  cData,bgColors), 3);
    }

    private void calcCorrParam(byte[] raw, byte[] bg, double[] aArr, double[] bArr, int f,int th) {
        double sumX = 0, sumY = 0, sumXX = 0, sumXY = 0;
        long n = 0;
        int imgSize = this.width*this.height;
        for (int i = 0; i < imgSize; i++) {
            int currVal = raw[i+(f-1)*imgSize] & 0xff;
            int  refVal = bg[i] & 0xff;
            if (Math.abs(currVal - refVal) < th) {
                sumX  += currVal; sumY += refVal;
                sumXX += currVal * currVal; sumXY += currVal * refVal; n++;
            }
        }
        for (int i = 0; i < imgSize; i++) {
            aArr[i+(f-1)*imgSize] = 1.0; bArr[i+(f-1)*imgSize] = 0.0;
        }
        if (n > 100) {
            double denom = (n * sumXX - sumX * sumX);
            for (int i = 0; i < imgSize; i++) {
                if (Math.abs(denom) > 1e-6) {
                    aArr[i+(f-1)*imgSize] = (n * sumXY - sumX * sumY) / denom;
                    bArr[i+(f-1)*imgSize] = (sumY - aArr[i+(f-1)*imgSize] * sumX) / n;
                }
            }
        }
    }

    public ImagePlus restore(){
        if(this.zData != null) return super.restore();
        if (this.zBgImg == null || this.zDiffImg == null) {
            throw new IllegalStateException("圧縮データが存在しません。");
        }
        int numPixels = this.width * this.height;
        byte[] bgData   = Zstd.decompress(this.zBgImg, numPixels);
        byte[] diffData = Zstd.decompress(this.zDiffImg, this.size * numPixels);
        ImageStack restoredStack = new ImageStack(this.width, this.height);
        for (int f = 1; f <= this.size; f++) {
            byte[] imgData = Arrays.copyOfRange(diffData, (f-1)*numPixels, f*numPixels);
            for(int i = 0 ; i < imgData.length ; i ++) imgData[i] += bgData[i];
            restoredStack.addSlice("Restored_" + f, new ByteProcessor(this.width,this.height,imgData,null));
        }
        return new ImagePlus("Restored_Sub_Video", restoredStack);        
    }
}
