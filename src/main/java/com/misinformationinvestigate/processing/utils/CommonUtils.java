package com.misinformationinvestigate.processing.utils;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.security.spec.ECField;
import java.util.Arrays;

public class CommonUtils {
    public static short[] mergeTwoShortArrays(short[] arr1, short[] arr2){
        short[] mergedArray = new short[arr1.length + arr2.length];
        System.arraycopy(arr1, 0, mergedArray, 0, arr1.length);
        System.arraycopy(arr2, 0, mergedArray, arr1.length, arr2.length);
//        System.out.println("after merge:" + Arrays.toString(Arrays.copyOfRange(mergedArray, mergedArray.length - 512, mergedArray.length)));
        return mergedArray;
    }
    // little endian always?
    public static short[] convertByteToShortArray(byte[] arr){
        ShortBuffer shortBuffer = ByteBuffer.wrap(arr).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        short[] convertedArray = new short[arr.length / 2];
        shortBuffer.get(convertedArray);
        return convertedArray;
    }
    public static float[] convertShortToFloatArray(short[] arr){
        float[] convertedArray = new float[arr.length];
        for(int ind = 0; ind < arr.length; ind++){
            convertedArray[ind] = (1.0f * arr[ind]) / 32768f;
        }
        return convertedArray;
    }

    public static void createAudioFile(short[] arr) {
        byte[] bytes = new byte[arr.length * 2];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(arr);
        File out = new File("C:\\Users\\geeth\\out10.wav");

        final boolean bigEndian = false;
        final boolean signed = true;

        final int bits = 16;
        final int channels = 1;

        try {
            AudioFormat format = new AudioFormat(16000f, bits, channels, signed, bigEndian);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            AudioInputStream audioInputStream = new AudioInputStream(bais, format, arr.length);
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, out);
            audioInputStream.close();
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
    }

}
