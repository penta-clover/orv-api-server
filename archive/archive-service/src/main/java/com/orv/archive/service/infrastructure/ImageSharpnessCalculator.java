package com.orv.archive.service.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;

import static org.bytedeco.opencv.global.opencv_core.CV_64F;
import static org.bytedeco.opencv.global.opencv_core.meanStdDev;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.Laplacian;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;

@Component
@Slf4j
public class ImageSharpnessCalculator {

    /**
     * Laplacian variance를 사용하여 이미지의 선명도를 계산합니다.
     * 값이 높을수록 선명한 이미지입니다.
     */
    public double calculate(BufferedImage image) {
        try (Java2DFrameConverter java2DConverter = new Java2DFrameConverter();
             OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat()) {

            Mat src = matConverter.convert(java2DConverter.convert(image));
            Mat gray = new Mat();
            Mat laplacian = new Mat();
            Mat mean = new Mat();
            Mat stddev = new Mat();

            try {
                cvtColor(src, gray, COLOR_BGR2GRAY);
                Laplacian(gray, laplacian, CV_64F);
                meanStdDev(laplacian, mean, stddev);

                try (Indexer indexer = stddev.createIndexer()) {
                    double stddevValue = indexer.getDouble(0);
                    return stddevValue * stddevValue;
                }
            } finally {
                src.close();
                stddev.close();
                mean.close();
                laplacian.close();
                gray.close();
            }
        } catch (Exception e) {
            log.warn("Failed to calculate sharpness, returning 0.0", e);
            return 0.0;
        }
    }
}
