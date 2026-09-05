import ij.*;
import ij.plugin.ZProjector;
import ij.process.*;
import java.util.Arrays;

public class ZBGImage extends ZstdImage {

    public ZBGImage() { super(); }
    public ZBGImage(ImagePlus imp) { compress(imp, new NoLossStrategy(), 16, 16, ZProjector.MEDIAN_METHOD, 3);}
    public ZBGImage(ImagePlus imp, CompressionStrategy strategy) { compress(imp, strategy, 16, 16, ZProjector.MEDIAN_METHOD, 3);}

    public void compress(ImagePlus srcImp, CompressionStrategy strategy, int motionTh, int noiseTh, int projectionMethod, int zstdLevel) {
        this.strategy = strategy;
        this.zStdLevel = zstdLevel;
        byte[] rawData = setImagePlus(srcImp);

        // 1. Create a reference background image using ZProjector (median projection)
        ZProjector zp = new ZProjector(srcImp);
        zp.setMethod(projectionMethod);
        zp.doProjection();
        ImageProcessor refProcessor = zp.getProjection().getProcessor();
        byte[] refData = (byte[]) refProcessor.getPixels();

        // 2. Calculate correlation parameters for each frame (for memory efficiency, process as local arrays)
        int imgSize = this.width * this.height;
        double[] aArr = new double[this.size];
        double[] bArr = new double[this.size];
        for (int f = 1; f <= this.size; f++) calcCorrParam(rawData, refData, aArr, bArr, f, motionTh);        
        
        // 3. Calculate illumination correction parameters for each frame and subtract background 
        byte[] cData = new byte[rawData.length];
        for (int f = 1; f <= this.size; f++) {
            double a = aArr[f - 1];
            double b = bArr[f - 1];
            int frameOffset = (f - 1) * imgSize;
            
            for (int i = 0; i < imgSize; i++) {
                int currVal = rawData[frameOffset + i] & 0xff;
                
                double corrRaw = a * currVal + b;
                int bgData = refData[i] & 0xff; 
                double exactDiff = corrRaw - bgData;
                
                int qDiff = (int) Math.round(exactDiff);
                if (Math.abs(qDiff) < noiseTh) qDiff = 0;            
                
                int finalValue = qDiff + 128;
                if (finalValue > 255) finalValue = 255;
                if (finalValue < 0)   finalValue = 0;
                
                cData[frameOffset + i] = (byte) finalValue;
            }
        } 

        // 4.Reconstruct a new ImageStack with the difference frames and the reference background
        ImageStack diffStack = new ImageStack(this.width, this.height);
        for (int f = 1; f <= this.size; f++) {
            byte[] frameData = Arrays.copyOfRange(cData, (f - 1) * imgSize, f * imgSize);
            diffStack.addSlice("Diff_" + f, new ByteProcessor(this.width, this.height, frameData, null));
        }
        diffStack.addSlice("Background", refProcessor);
        this.compressedSize = this.size + 1; 
        
        compressBytes(diffStack);
    }

    private void calcCorrParam(byte[] raw, byte[] bg, double[] aArr, double[] bArr, int f, int th) {
        double sumX = 0, sumY = 0, sumXX = 0, sumXY = 0;
        long n = 0;
        int imgSize = this.width * this.height;
        for (int i = 0; i < imgSize; i++) {
            int currVal = raw[i + (f - 1) * imgSize] & 0xff; // X (現在フレーム)
            int refVal = bg[i] & 0xff;                       // Y (基準背景)
            if (Math.abs(currVal - refVal) < th) {
                sumX  += currVal; sumXX += currVal * currVal;
                sumY  +=  refVal; sumXY += currVal * refVal; 
                n++;
            }
        }        
        aArr[f - 1] = 1.0; 
        bArr[f - 1] = 0.0;        
        if (n > 100) {
            double denom = (n * sumXX - sumX * sumX);
            if (Math.abs(denom) > 1e-6) {
                aArr[f - 1] = (n * sumXY - sumX * sumY) / denom;
                bArr[f - 1] = (sumY - aArr[f - 1] * sumX) / n;
            }
        }
    }

    @Override
    public ImagePlus restore() {
        ImageStack decompressStack = decompressPackedBytes();
        ImageStack restoredStack = new ImageStack(this.width, this.height);
        byte[] bgData = (byte[]) decompressStack.getProcessor(this.size + 1).getPixels(); 
        for (int f = 1; f <= this.size; f++) {
            byte[] diffData = (byte[]) decompressStack.getProcessor(f).getPixels();            
            for (int i = 0; i < diffData.length; i++) {
                int qDiff = (diffData[i] & 0xff) - 128;
                int restoredPixel = qDiff + (bgData[i] & 0xff);                 
                if (restoredPixel > 255) restoredPixel = 255;
                if (restoredPixel < 0)   restoredPixel = 0;                
                diffData[i] = (byte) restoredPixel;
            }
            restoredStack.addSlice("Restored_" + f, new ByteProcessor(this.width, this.height, diffData, null));
        }
        return new ImagePlus("Restored_BGSub_Video", restoredStack);        
    }
}


