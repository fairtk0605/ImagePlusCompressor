import ij.*;
import ij.process.*;
import com.github.luben.zstd.Zstd;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY) 
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@c")
public class ZstdImage {
    public int width, height, size;
    public byte[] zData; 

    public ZstdImage() {}
    public ZstdImage(ImagePlus imp){ compress(imp,3);}

    @JsonIgnore
    protected byte[] setImagePlus(ImagePlus srcImp) {
        if (srcImp == null || srcImp.getStackSize() == 0) {
            throw new IllegalArgumentException("有効な8bitモノクロ画像(またはスタック)が必要です。");
        }
        this.size = srcImp.getStackSize();
        this.width = srcImp.getWidth();
        this.height = srcImp.getHeight();
        ImageStack stack = srcImp.getStack();

        int num = this.width*this.height;
        byte[] rawData = new byte[this.size * num];
        for (int f = 1; f <= this.size; f++) 
            System.arraycopy(stack.getProcessor(f).getPixels(), 0, rawData, (f - 1) * num, num);

        return rawData;
    }

    public void compress(ImagePlus srcImp, int zstdLevel) {
        byte[] rawVolume = setImagePlus(srcImp);
        this.zData = Zstd.compress(rawVolume, zstdLevel);
    }

    public ImagePlus restore() {
        if (this.zData == null) throw new IllegalStateException("データがありません。");
        
        int numPixels = this.width * this.height;
        byte[] rawVolume = Zstd.decompress(this.zData, this.size * numPixels);
        ImageStack stack = new ImageStack(this.width, this.height);
        
        for (int f = 1; f <= this.size; f++) {
            ByteProcessor ip = new ByteProcessor(this.width, this.height);
            System.arraycopy(rawVolume, (f - 1) * numPixels, ip.getPixels(), 0, numPixels);
            stack.addSlice("Frame_" + f, ip);
        }
        return new ImagePlus("Restored_Raw", stack);
    }
}
