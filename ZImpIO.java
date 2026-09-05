import java.io.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import ij.IJ;

public class ZImpIO {

    private final ObjectMapper mapper;

    public ZImpIO() {
        this.mapper = new ObjectMapper(new MessagePackFactory());
    }

    public void write(ZstdImage image, File file) {
        if (image == null) {
            IJ.log("【ZImpIOエラー】保存する画像オブジェクトが null です。");
            return;
        }
        if (file == null) {
            IJ.log("【ZImpIOエラー】保存先ファイルが指定されていません（null）。");
            return;
        }
        
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
            mapper.writeValue(os, image);
        } catch (IOException e) {
            e.printStackTrace();
            IJ.log("【ZImpIOエラー】画像の保存に失敗しました: " + e.getMessage());
        }
    }

    public void write(ZstdImage image, String filePath) {
        if (filePath == null) {
            IJ.log("【ZImpIOエラー】保存先パスが指定されていません（null）。");
            return;
        }
        write(image, new File(filePath));
    }

    public ZstdImage read(File file) {
        if (file == null || !file.exists()) {
            IJ.log("【ZImpIOエラー】指定されたファイルが存在しないか、または null です。");
            return null;
        }
        
        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            return mapper.readValue(is, ZstdImage.class);
        } catch (IOException e) {
            e.printStackTrace();
            IJ.log("【ZImpIOエラー】画像の読み込みに失敗しました: " + e.getMessage());
            return null;
        }
    }

    public ZstdImage read(String filePath) {
        if (filePath == null) {
            IJ.log("【ZImpIOエラー】読み込み元のパスが指定されていません（null）。");
            return null;
        }
        return read(new File(filePath));
    }
}
