import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

import ij.process.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@strategy")
public interface CompressionStrategy {
    byte[] process(ImageProcessor ip);
    ImageProcessor deprocess(byte[] processedData, int width, int height);
}

class NoLossStrategy implements CompressionStrategy {
    @Override
    public byte[] process(ImageProcessor ip) { return (byte[]) ip.getPixels(); }
    @Override
    public ImageProcessor deprocess(byte[] processedData, int width, int height) {
        return new ByteProcessor(width, height,processedData);
    }
}

class QuantizationStrategy implements CompressionStrategy {
    public int colors;

    public QuantizationStrategy() {}
    public QuantizationStrategy(int colors) {
        this.colors = Math.min(256, Math.max(2, colors));
    }

    @Override
    public byte[] process(ImageProcessor ip) {
        byte[] rawData = (byte[]) ip.getPixels();
        byte[] qData = new byte[rawData.length];
        float scale = (colors - 1) / 255.0f;
        float restoreScale = 255.0f / (colors - 1);
        for (int i = 0; i < rawData.length; i++) {
            int quantized = Math.round((rawData[i] & 0xff) * scale);
            qData[i] = (byte) Math.round(quantized * restoreScale);
        }
        return qData;
    }

    @Override
    public ImageProcessor deprocess(byte[] processedData, int width, int height) {
        return new ByteProcessor(width, height, processedData);
    }
}

class JpegStrategy implements CompressionStrategy {
    public float jpegQuality; // 0.0 ~ 1.0

    public JpegStrategy() {}
    public JpegStrategy(float jpegQuality) { 
        this.jpegQuality = Math.min(1.0f, Math.max(0.0f, jpegQuality)); 
    }

    @Override
    public byte[] process(ImageProcessor ip) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        byte[] rawPixels = (byte[]) ip.getPixels();
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);        
        bufferedImage.getRaster().setDataElements(0, 0, width, height, rawPixels);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try { ImageIO.write(bufferedImage, "jpg", baos);}
        catch (IOException e) {
            e.printStackTrace(); 
            ij.IJ.log("JPEG変換に失敗しました: " + e.getMessage());
            return null;
        }
        return baos.toByteArray();
    }

    @Override
    public ImageProcessor deprocess(byte[] processedData, int width, int height) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(processedData));
            return new ByteProcessor(image);
        } catch (IOException e) {
            e.printStackTrace();
            ij.IJ.log("JPEG展開に失敗しました: " + e.getMessage());
            return null;
        }
    }
}