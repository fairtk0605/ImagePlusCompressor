import ij.*;
import ij.plugin.*;

public class ZImpSample implements PlugIn {    
    public void run(String arg) {
        ImagePlus imp = IJ.getImage();

        ZstdImage zImp0 = new ZstdImage(imp, new NoLossStrategy());
        testIO(zImp0, "test_zimp0.zimp");
        ZstdImage zImp1 = new ZstdImage(imp, new QuantizationStrategy(64));
        testIO(zImp1, "test_zimp1.zimp");

        ZstdImage zImp2 = new ZstdImage(imp, new JpegStrategy(0.85f));
        testIO(zImp2, "test_zimp2.zimp");

        ZBGImage zImp00 = new ZBGImage(imp, new NoLossStrategy());
        testIO(zImp00, "test_zimp00.zimp");

        ZBGImage zImp01 = new ZBGImage(imp, new QuantizationStrategy(64));
        testIO(zImp01, "test_zimp01.zimp");

        ZBGImage zImp02 = new ZBGImage(imp, new JpegStrategy(0.85f));
        testIO(zImp02, "test_zimp02.zimp");
        
    }

    void testIO(ZstdImage zImp, String filePath) {
        IJ.log("Size:"+zImp.zData.length);
        ZImpIO zImpIO = new ZImpIO();
        zImpIO.write(zImp, filePath);
        ZstdImage readZImp = zImpIO.read(filePath);
        ImagePlus restoredImp = readZImp.restore();
        restoredImp.show();
    }

}
