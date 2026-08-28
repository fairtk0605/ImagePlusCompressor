package com.kijm.utils;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import java.io.*;
import java.util.Base64;
import java.util.zip.*;
import com.github.luben.zstd.*;
import org.tukaani.xz.*;

public class ImpCompressor {
    public enum Algorithm { ZIP, ZSTD, LZMA }

    public static String compress(ImagePlus imp){ return compress(imp,Algorithm.ZSTD);}

    public static String compress(ImagePlus imp, Algorithm algo) {
        int w = imp.getWidth(), h = imp.getHeight();
        byte[] pixels = (byte[]) imp.getProcessor().getPixels();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        
        try (OutputStream os = (algo == Algorithm.ZIP) ? new DeflaterOutputStream(bos) :
                               (algo == Algorithm.ZSTD) ? new ZstdOutputStream(bos) : 
                               new XZOutputStream(bos, new LZMA2Options())) {
            os.write(pixels);
        } catch (IOException e) {
            throw new RuntimeException(e); 
        }
        
        byte[] comp = bos.toByteArray();
        String b64 = Base64.getEncoder().encodeToString(comp);
        return String.format("W%dH%dA%sR%.4fB64:%s", w, h, algo.name(), (double) comp.length / pixels.length, b64);
    }

    public static ImagePlus decompress(String str) {
        int colon = str.indexOf(':');
        String hd = str.substring(0, colon);
        int w = Integer.parseInt(hd.substring(hd.indexOf('W') + 1, hd.indexOf('H')));
        int h = Integer.parseInt(hd.substring(hd.indexOf('H') + 1, hd.indexOf('A')));
        Algorithm algo = Algorithm.valueOf(hd.substring(hd.indexOf('A') + 1, hd.indexOf('R')));

        byte[] comp = Base64.getDecoder().decode(str.substring(colon + 1));
        byte[] pixels = new byte[w * h];

        try (InputStream is = (algo == Algorithm.ZIP) ? new InflaterInputStream(new ByteArrayInputStream(comp)) :
                              (algo == Algorithm.ZSTD) ? new ZstdInputStream(new ByteArrayInputStream(comp)) : 
                              new XZInputStream(new ByteArrayInputStream(comp))) {
            int offset = 0, read;
            while (offset < pixels.length && (read = is.read(pixels, offset, pixels.length - offset)) != -1) {
                offset += read;
            }
        } catch (IOException e) {
            throw new RuntimeException(e); 
        }

        return new ImagePlus("Decoded", new ByteProcessor(w, h, pixels));
    }
}
