package com.misinformationinvestigate.processing.helper;

import org.apache.flink.streaming.api.functions.source.SourceFunction;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;

import static com.misinformationinvestigate.processing.utils.CommonUtils.convertByteToShortArray;
public class RandomLiveStreamGenerator implements SourceFunction<String> {

    public static final int MILLIS_SLEEP = 8;
    private volatile boolean running = true;

    @Override
    public void run(SourceContext<String> sourceContext) throws Exception {

        try {

            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File("C:\\Users\\geeth\\out.wav"));
            int framesRead;
            int count = 0;
            byte[] audioData = new byte[1024];
            do {
                framesRead = audioInputStream.read(audioData);
                sourceContext.collect("{source:\"" + count + "\",audio_data:" + Arrays.toString(convertByteToShortArray(audioData)) +",session_id:\"session_1\"}");
//                System.out.println("source:" + Arrays.toString(convertByteToShortArray(audioData)));
                Thread.sleep(MILLIS_SLEEP);
                count++;
            }while(framesRead != -1);

        }catch(Exception ex){
                System.out.println(ex.getMessage());
        }
    }

    @Override
    public void cancel() {
        running = false;
    }
}
