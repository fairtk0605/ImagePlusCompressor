import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import java.io.File;
import java.io.IOException;

import ij.*;
import ij.plugin.*;

public class ZImpSample implements PlugIn {    
    public void run(String arg) {
        ImagePlus imp = IJ.getImage();

        ZstdImage zImp0 = new ZstdImage(imp);
        ZQImage   zImp1 = new ZQImage(imp,64);
        ZQBGImage zImp2 = new ZQBGImage(imp);

        saveAndreadAndShowZimp("F0", zImp0);
        saveAndreadAndShowZimp("F1", zImp1);
        saveAndreadAndShowZimp("F2", zImp2);
    }

    public void saveAndreadAndShowZimp(String file,ZstdImage zimp){
        ObjectMapper mapper = new ObjectMapper(new MessagePackFactory());
        File f0 = new File(file + ".zimp");
        try{mapper.writeValue(f0,zimp);}
        catch (IOException e) { e.printStackTrace();}
        try{ZstdImage zImp = mapper.readValue(f0, ZstdImage.class);
            zImp.restore().show();
        }
        catch (IOException e) { e.printStackTrace();}
    }

}
