import ij.*;

import java.io.*;
import java.nio.*;

import com.github.luben.zstd.Zstd;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY) 
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@c")
public class ZstdImage {
    public int width, height, size;
    public int compressedSize,dataSize,zStdLevel;
    public byte[] zData; 
    public CompressionStrategy strategy; 

    // compress ImageProcessor to byte[] and decompress byte[] to ImageProcessor
    // byte[] have size and data

    public ZstdImage() {}

    public ZstdImage(ImagePlus imp) { compress(imp, new NoLossStrategy(), 3); }
    public ZstdImage(ImagePlus imp, CompressionStrategy strategy) { compress(imp, strategy, 3);}

    @JsonIgnore
    protected byte[] setImagePlus(ImagePlus srcImp) {
        if (srcImp == null || srcImp.getStackSize() == 0) {
            throw new IllegalArgumentException("有効な8bitモノクロ画像(またはスタック)が必要です。");
        }
        this.size = srcImp.getStackSize();
        this.width = srcImp.getWidth();
        this.height = srcImp.getHeight();
        this.compressedSize = this.size;
        ImageStack stack = srcImp.getStack();
  
        int num = this.width*this.height;
        byte[] rawData = new byte[this.size * num];
        for (int f = 1; f <= this.size; f++) 
            System.arraycopy(stack.getProcessor(f).getPixels(), 0, rawData, (f - 1) * num, num);
        return rawData;
    }

    protected ImageStack prepareStack(ImagePlus srcImp) { return srcImp.getStack();}

    public void compress(ImagePlus srcImp, CompressionStrategy strategy, int zstdLevel) {
        this.strategy = strategy;
        this.zStdLevel = zstdLevel;
        setImagePlus(srcImp);
        compressBytes(srcImp.getStack());
    }

    protected void compressBytes(ImageStack is) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (int f = 1; f <= this.compressedSize; f++) {
                byte[] data = strategy.process(is.getProcessor(f));
                baos.write(ByteBuffer.allocate(4).putInt(data.length).array());
                baos.write(data);
            }
            this.dataSize = baos.toByteArray().length;
            this.zData = Zstd.compress(baos.toByteArray(), this.zStdLevel);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("圧縮に失敗しました。", e);
        }
    }

    protected ImageStack decompressPackedBytes() {
        if (this.zData == null) throw new IllegalStateException("データがありません。");
        ImageStack stack = new ImageStack(this.width, this.height);
        
        ByteBuffer buffer = ByteBuffer.wrap(Zstd.decompress(this.zData, this.dataSize));
        for(int f = 1 ; f <= this.compressedSize ; f++) {
            int size = buffer.getInt(); 
            byte[] chunk = new byte[size];
            buffer.get(chunk); 
            stack.addSlice("Frame_" + f,strategy.deprocess(chunk,this.width,this.height));
        }
        return stack;
    }

    public ImagePlus restore() {
        return new ImagePlus("Restored_Image", decompressPackedBytes());
    }
}
