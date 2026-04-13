package org.telegram.ui;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

public class AudioVectorExtractor {

    // Вызывается из SmartRadioEngine (getTrackVector)
    public static float[] extractVector(MessageObject track, OrtEnvironment env, OrtSession session) {
        try {
            if (track == null || track.messageOwner.attachPath == null) return null;
            File audioFile = new File(track.messageOwner.attachPath);
            if (!audioFile.exists()) return null;

            // 1. Декодируем кусок аудио (3-5 секунд)
            float[] pcmData = decodeAudioToPCM(audioFile.getAbsolutePath());
            if (pcmData == null) return null;

            // 2. В ИДЕАЛЕ: Здесь PCM конвертируется в Мел-Спектрограмму (librosa analog)
            // mel_spect = MathUtils.computeMelSpectrogram(pcmData, 16000, 512, 256, 96);
            // Для примера создаем матрицу нужного размера [1, 1, frames, 96]
            int frames = 300; 
            float[][][][] melSpectrogram = new float[1][1][frames][96];
            // Заполняем фейковыми данными на основе PCM, пока нет Java-FFT
            for (int i=0; i<frames; i++) {
                for (int j=0; j<96; j++) {
                    melSpectrogram[0][0][i][j] = pcmData[(i * 96 + j) % pcmData.length];
                }
            }

            // 3. Прогон через ONNX
            FloatBuffer floatBuffer = FloatBuffer.allocate(frames * 96);
            for (int i = 0; i < frames; i++) {
                for (int j = 0; j < 96; j++) {
                    floatBuffer.put(melSpectrogram[0][0][i][j]);
                }
            }
            floatBuffer.rewind();

            long[] shape = {1, 1, frames, 96};
            OnnxTensor tensor = OnnxTensor.createTensor(env, floatBuffer, shape);
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("melspectrogram", tensor); // Проверь имя входа через Netron

            OrtSession.Result result = session.run(inputs);
            float[][] output = (float[][]) result.get(0).getValue(); // [frames, 400]
            
            // Усредняем по времени
            float[] finalVector = new float[400];
            for (int i = 0; i < output.length; i++) {
                for (int j = 0; j < 400; j++) {
                    finalVector[j] += output[i][j];
                }
            }
            
            float norm = 0;
            for (int j = 0; j < 400; j++) {
                finalVector[j] /= output.length;
                norm += finalVector[j] * finalVector[j];
            }
            norm = (float) Math.sqrt(norm);
            for (int j = 0; j < 400; j++) finalVector[j] /= norm; // Нормализация

            return finalVector;

        } catch (Exception e) {
            FileLog.e("SoundGram AudioExtractor Error", e);
            return null;
        }
    }

    private static float[] decodeAudioToPCM(String filePath) {
        // Упрощенный декодер: берем MediaExtractor, читаем 100 пакетов
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(filePath);
            int trackIndex = -1;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    trackIndex = i;
                    break;
                }
            }
            if (trackIndex == -1) return null;
            extractor.selectTrack(trackIndex);

            ByteBuffer buffer = ByteBuffer.allocate(8192);
            float[] pcm = new float[16000 * 3]; // На 3 секунды
            int pcmIndex = 0;

            for (int i = 0; i < 100; i++) { // Читаем ограниченно
                int read = extractor.readSampleData(buffer, 0);
                if (read < 0) break;
                
                buffer.rewind();
                while (buffer.hasRemaining() && pcmIndex < pcm.length) {
                    short sample = (short) ((buffer.get() & 0xFF) | (buffer.get() << 8));
                    pcm[pcmIndex++] = sample / 32768.0f; // Нормализуем -1..1
                }
                extractor.advance();
            }
            extractor.release();
            return pcm;
        } catch (Exception e) {
            extractor.release();
            return null;
        }
    }
}

