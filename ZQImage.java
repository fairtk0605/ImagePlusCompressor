import ij.*;
import com.github.luben.zstd.Zstd;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class ZQImage extends ZstdImage {
    public int nCol;

    public ZQImage() {
        super();
    }

    public ZQImage(ImagePlus imp, int col) {
        compress(imp, col, 3);
    }

    public void compress(ImagePlus srcImp, int colors, int zstdLevel) {
        this.nCol = Math.min(256, Math.max(2, colors));
        byte[] rawData = setImagePlus(srcImp);
        this.zData = Zstd.compress(getQuantized(rawData, this.nCol), zstdLevel);
    }

    @JsonIgnore
    protected byte[] getQuantized(byte[] rawData, int col) {
        byte[] qData = new byte[rawData.length];
        float scale = (col - 1) / 255.0f;
        float restoreScale = 255.0f / (col - 1);
        for (int i = 0; i < rawData.length; i++) {
            int quantized = Math.round((rawData[i] & 0xff) * scale);
            qData[i] = (byte) Math.round(quantized * restoreScale);
        }
        return qData;
    }

}
