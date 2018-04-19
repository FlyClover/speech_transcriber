package test;

import com.google.protobuf.ByteString;
import com.mobvoi.speech.recognition.v1.SpeechGrpc;
import com.mobvoi.speech.recognition.v1.SpeechProto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class SpeechTest {
    public static void main(String... args) throws Exception {
        Logger logger = Logger.getLogger(SpeechTest.class.getName());

        if (args.length == 0) {
            logger.warning("Specify input wav path");
            return;
        }

        ManagedChannel channel =
                ManagedChannelBuilder.forAddress("jenkins-slave", 32769).usePlaintext(true).build();
        SpeechGrpc.SpeechBlockingStub blockingStub = SpeechGrpc.newBlockingStub(channel);

        String fileName = args[0];
        logger.info(fileName);
        Path path = Paths.get(fileName);
        byte[] data = Files.readAllBytes(path);
        ByteString audioBytes = ByteString.copyFrom(data);

        SpeechProto.RecognitionAudio audio =
                SpeechProto.RecognitionAudio.newBuilder().setContent(audioBytes).build();

        SpeechProto.RecognitionConfig config =
                SpeechProto.RecognitionConfig.newBuilder()
                        .setEncoding(SpeechProto.RecognitionConfig.Encoding.WAV16)
                        .setSampleRate(8000)
                        .setChannel(1)
                        .build();

        SpeechProto.RecognizeRequest request =
                SpeechProto.RecognizeRequest.newBuilder().setConfig(config).setAudio(audio).build();

        SpeechProto.RecognizeResponse response;
        try {
            response = blockingStub.recognize(request);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            return;
        }

        for (SpeechProto.SingleUtteranceResult result : response.getResultsList()) {
            SpeechProto.SpeechRecognitionAlternative alternative = result.getAlternatives(0);

            logger.log(Level.INFO, Float.toString(result.getStartTime()) + "-"
                            + Float.toString(result.getEndTime()) + " "
                            + alternative.getTranscript());
        }

        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}
