package com.orv.archive.service;

import com.orv.archive.domain.ImageMetadata;
import com.orv.archive.domain.InputStreamWithMetadata;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

/**
 * 영상 처리 관련 유틸리티 메서드를 제공합니다.
 * <p>
 * TODO: 영상 처리 작업을 별도 서버로 분리한 후 이 클래스를 제거해야 함.
 */
@Slf4j
public final class VideoProcessingUtils {

    private VideoProcessingUtils() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    /**
     * 영상에서 키 프레임을 추출합니다.
     *
     * @param grabber FFmpegFrameGrabber 인스턴스 (이미 시작된 상태여야 함)
     * @return 추출된 키 프레임 이미지, 추출 실패 시 빈 Optional
     */
    public static Optional<BufferedImage> extractKeyFrame(FFmpegFrameGrabber grabber) {
        Java2DFrameConverter frameConverter = new Java2DFrameConverter();

        try {
            Frame keyFrame = grabber.grabKeyFrame();

            if (keyFrame == null || keyFrame.image == null) {
                log.warn("No key frame available");
                return Optional.empty();
            }

            BufferedImage image = frameConverter.convert(keyFrame);

            if (image == null) {
                log.warn("Failed to convert frame to BufferedImage");
                return Optional.empty();
            }

            return Optional.of(image);
        } catch (Exception e) {
            log.error("Failed to extract key frame", e);
            return Optional.empty();
        } finally {
            frameConverter.close();
        }
    }

    /**
     * 영상의 재생 시간을 초 단위로 계산합니다.
     *
     * @param grabber FFmpegFrameGrabber 인스턴스 (이미 시작된 상태여야 함)
     * @return 영상 재생 시간 (초), 계산 실패 시 0.0
     */
    public static double calculateRunningTime(FFmpegFrameGrabber grabber) {
        try {
            long lengthInTime = grabber.getLengthInTime();
            if (lengthInTime > 0) {
                return lengthInTime / 1_000_000.0;
            }

            // 메타데이터가 없을 경우, 첫 프레임과 마지막 프레임의 timestamp를 이용해 계산
            grabber.setTimestamp(0);
            Frame frame;
            long firstTimestamp = -1;
            long lastTimestamp = -1;
            while ((frame = grabber.grabFrame()) != null) {
                if (frame.timestamp > 0) {
                    if (firstTimestamp == -1) {
                        firstTimestamp = frame.timestamp;
                    }
                    lastTimestamp = frame.timestamp;
                }
            }

            if (firstTimestamp != -1 && lastTimestamp != -1) {
                return (lastTimestamp - firstTimestamp) / 1_000_000.0;
            }

            return 0.0;
        } catch (Exception e) {
            log.error("Failed to calculate running time", e);
            return 0.0;
        }
    }

    /**
     * BufferedImage를 InputStreamWithMetadata로 변환합니다.
     *
     * @param image  변환할 이미지
     * @param format 이미지 포맷 (예: "jpg", "png")
     * @return 변환된 InputStreamWithMetadata
     * @throws IOException 변환 실패 시
     */
    public static InputStreamWithMetadata bufferedImageToInputStream(BufferedImage image, String format) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, outputStream);
            return new InputStreamWithMetadata(
                    new ByteArrayInputStream(outputStream.toByteArray()),
                    new ImageMetadata(
                            "image/" + format,
                            outputStream.size()
                    )
            );
        }
    }
}
