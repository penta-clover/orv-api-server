package lib

import static net.grinder.script.Grinder.grinder
import net.grinder.plugin.http.HTTPRequest
import net.grinder.plugin.http.HTTPResponse

/**
 * 미디어 파일 업로드/다운로드 헬퍼 클래스
 * Phase 1 Day 2에서 구현된 미디어 처리 클래스
 */
class MediaHelper {
    private HTTPRequest request
    private MetricsCollector metricsCollector
    
    /**
     * MediaHelper 생성자
     * @param request nGrinder HTTPRequest 인스턴스
     * @param metricsCollector 메트릭 수집기
     */
    MediaHelper(HTTPRequest request, MetricsCollector metricsCollector) {
        this.request = request
        this.metricsCollector = metricsCollector
    }
    
    /**
     * 비디오 파일 업로드 (시나리오 A 핵심 기능)
     * 7분 480p 영상 (~5MB) multipart/form-data 업로드
     * 
     * @param videoFile 업로드할 비디오 파일
     * @param storyboardId 선택된 스토리보드 UUID
     * @param authHeaders 인증 헤더 (Authorization: Bearer ...)
     * @return 업로드된 video_id 또는 null (실패 시)
     */
    String uploadVideo(File videoFile, String storyboardId, Map<String, String> authHeaders) {
        try {
            grinder.logger.info("Starting video upload: ${videoFile.name} for storyboard: ${storyboardId}")
            
            // multipart 요청 데이터 생성
            def multipartData = createMultipartRequest(videoFile, storyboardId)
            
            // HTTP 헤더 설정 (Content-Type은 자동 설정됨)
            Map<String, String> headers = new HashMap<>(authHeaders)
            
            // 업로드 요청 실행
            long startTime = System.currentTimeMillis()
            HTTPResponse response = request.POST(ApiEndpoints.VIDEO_UPLOAD, multipartData, headers)
            long responseTime = System.currentTimeMillis() - startTime
            
            // 메트릭 수집
            metricsCollector?.recordResponse("VIDEO_UPLOAD", responseTime, response.statusCode)
            
            if (response.statusCode == 200 || response.statusCode == 201) {
                // 응답에서 video_id 추출
                String responseBody = response.getText()
                String videoId = extractVideoIdFromResponse(responseBody)
                
                grinder.logger.info("Video upload successful: videoId=${videoId}, responseTime=${responseTime}ms")
                return videoId
                
            } else {
                String errorMsg = "Video upload failed: HTTP ${response.statusCode}"
                grinder.logger.error(errorMsg)
                metricsCollector?.logError("VIDEO_UPLOAD", errorMsg)
                
                // 재시도 로직
                if (shouldRetryUpload(response.statusCode)) {
                    grinder.logger.info("Retrying video upload...")
                    return retryUploadVideo(videoFile, storyboardId, authHeaders, 1)
                }
                
                return null
            }
            
        } catch (Exception e) {
            String errorMsg = "Video upload error: ${e.message}"
            grinder.logger.error(errorMsg)
            metricsCollector?.logError("VIDEO_UPLOAD", errorMsg)
            return null
        }
    }
    
    /**
     * 오디오 스트리밍 시뮬레이션 (시나리오 B 핵심 기능)
     * HTTP Range Request를 사용한 Progressive Download 구현
     * 
     * @param audioUrl S3 직접 접근 URL 또는 스트리밍 엔드포인트
     * @param durationSeconds 스트리밍 지속 시간 (초)
     * @return 스트리밍 성공 여부
     */
    boolean streamAudio(String audioUrl, int durationSeconds) {
        try {
            grinder.logger.info("Starting audio streaming: ${audioUrl} for ${durationSeconds} seconds")
            
            long streamingStartTime = System.currentTimeMillis()
            long streamingEndTime = streamingStartTime + (durationSeconds * 1000)
            
            int chunkCount = 0
            boolean streamingSuccess = true
            
            // Progressive Download 시뮬레이션
            while (System.currentTimeMillis() < streamingEndTime) {
                // Range Request 헤더 생성
                Map<String, String> rangeHeaders = createRangeHeaders(chunkCount)
                
                // 청크 다운로드 요청
                long chunkStartTime = System.currentTimeMillis()
                HTTPResponse response = request.GET(audioUrl, rangeHeaders)
                long chunkResponseTime = System.currentTimeMillis() - chunkStartTime
                
                // 메트릭 수집
                String metricName = "AUDIO_STREAM_CHUNK_${chunkCount}"
                metricsCollector?.recordResponse(metricName, chunkResponseTime, response.statusCode)
                
                if (response.statusCode == 206 || response.statusCode == 200) {
                    // 정상적인 청크 수신
                    chunkCount++
                    
                    if (chunkCount % 10 == 0) {
                        grinder.logger.info("Audio streaming progress: ${chunkCount} chunks received")
                    }
                    
                } else {
                    String errorMsg = "Audio chunk ${chunkCount} failed: HTTP ${response.statusCode}"
                    grinder.logger.error(errorMsg)
                    metricsCollector?.logError("AUDIO_STREAMING", errorMsg)
                    streamingSuccess = false
                    break
                }
                
                // 다음 청크까지 대기 (100ms 간격)
                Thread.sleep(TestConfig.STREAMING_INTERVAL_MS)
            }
            
            long totalStreamingTime = System.currentTimeMillis() - streamingStartTime
            grinder.logger.info("Audio streaming completed: ${chunkCount} chunks, ${totalStreamingTime}ms total")
            
            return streamingSuccess
            
        } catch (Exception e) {
            String errorMsg = "Audio streaming error: ${e.message}"
            grinder.logger.error(errorMsg)
            metricsCollector?.logError("AUDIO_STREAMING", errorMsg)
            return false
        }
    }
    
    /**
     * nGrinder에 최적화된 multipart/form-data 요청 데이터 생성
     * 실제 nGrinder HTTP 플러그인 API에 맞게 구현
     * 
     * @param videoFile 업로드할 비디오 파일
     * @param storyboardId 스토리보드 UUID
     * @return nGrinder 호환 multipart 요청 데이터
     */
    private def createMultipartRequest(File videoFile, String storyboardId) {
        try {
            // 파일 존재 여부 확인
            if (!videoFile.exists()) {
                throw new RuntimeException("Video file not found: ${videoFile.absolutePath}")
            }
            
            // nGrinder multipart 데이터 구조 (개선된 버전)
            def multipartData = [:]
            
            // 파일 부분
            multipartData['file'] = [
                'name': 'file',
                'filename': videoFile.name,
                'data': videoFile.bytes,
                'contentType': 'video/mp4'
            ]
            
            // 메타데이터 부분
            multipartData['storyboardId'] = [
                'name': 'storyboardId', 
                'data': storyboardId.getBytes('UTF-8'),
                'contentType': 'text/plain'
            ]
            
            grinder.logger.info("Created multipart request: file=${videoFile.name} (${videoFile.length()} bytes), storyboardId=${storyboardId}")
            
            return multipartData
            
        } catch (Exception e) {
            grinder.logger.error("Failed to create multipart request: ${e.message}")
            throw e
        }
    }
    
    /**
     * HTTP Range Request 헤더 생성
     * 
     * @param chunkIndex 청크 인덱스
     * @return Range 헤더가 포함된 헤더 맵
     */
    private Map<String, String> createRangeHeaders(int chunkIndex) {
        long chunkSize = TestConfig.AUDIO_CHUNK_SIZE  // 64KB
        long startByte = chunkIndex * chunkSize
        long endByte = startByte + chunkSize - 1
        
        return [
            "Range": "bytes=${startByte}-${endByte}",
            "Accept": "audio/*"
        ]
    }
    
    /**
     * 업로드 재시도 여부 판단
     * 
     * @param statusCode HTTP 상태 코드
     * @return 재시도 가능 여부
     */
    private boolean shouldRetryUpload(int statusCode) {
        // 5xx 서버 에러 또는 408 타임아웃의 경우 재시도
        return statusCode >= 500 || statusCode == 408
    }
    
    /**
     * 비디오 업로드 재시도
     * 
     * @param videoFile 업로드할 비디오 파일
     * @param storyboardId 스토리보드 UUID
     * @param authHeaders 인증 헤더
     * @param retryCount 현재 재시도 횟수
     * @return 업로드된 video_id 또는 null (실패 시)
     */
    private String retryUploadVideo(File videoFile, String storyboardId, Map<String, String> authHeaders, int retryCount) {
        if (retryCount >= TestConfig.MAX_RETRY_COUNT) {
            grinder.logger.error("Video upload failed after ${TestConfig.MAX_RETRY_COUNT} retries")
            return null
        }
        
        try {
            // 재시도 전 대기 (지수 백오프)
            long waitTime = Math.pow(2, retryCount) * 1000  // 2^retryCount 초
            Thread.sleep(waitTime)
            
            grinder.logger.info("Retrying video upload (attempt ${retryCount + 1}/${TestConfig.MAX_RETRY_COUNT})")
            
            // 재시도 실행
            def multipartData = createMultipartRequest(videoFile, storyboardId)
            Map<String, String> headers = new HashMap<>(authHeaders)
            
            long startTime = System.currentTimeMillis()
            HTTPResponse response = request.POST(ApiEndpoints.VIDEO_UPLOAD, multipartData, headers)
            long responseTime = System.currentTimeMillis() - startTime
            
            // 메트릭 수집 (재시도 표시)
            metricsCollector?.recordResponse("VIDEO_UPLOAD_RETRY_${retryCount}", responseTime, response.statusCode)
            
            if (response.statusCode == 200 || response.statusCode == 201) {
                String responseBody = response.getText()
                String videoId = extractVideoIdFromResponse(responseBody)
                
                grinder.logger.info("Video upload retry successful: videoId=${videoId}, attempt=${retryCount + 1}")
                return videoId
                
            } else if (shouldRetryUpload(response.statusCode)) {
                // 재귀적 재시도
                return retryUploadVideo(videoFile, storyboardId, authHeaders, retryCount + 1)
                
            } else {
                // 재시도 불가능한 에러
                String errorMsg = "Video upload retry failed with non-retryable error: HTTP ${response.statusCode}"
                grinder.logger.error(errorMsg)
                metricsCollector?.logError("VIDEO_UPLOAD_RETRY", errorMsg)
                return null
            }
            
        } catch (Exception e) {
            String errorMsg = "Video upload retry error (attempt ${retryCount + 1}): ${e.message}"
            grinder.logger.error(errorMsg)
            metricsCollector?.logError("VIDEO_UPLOAD_RETRY", errorMsg)
            
            // 예외 발생 시에도 재시도 시도
            return retryUploadVideo(videoFile, storyboardId, authHeaders, retryCount + 1)
        }
    }
    
    /**
     * 응답에서 video_id 추출
     * JSON 응답 파싱하여 videoId 또는 id 필드 추출
     * 
     * @param responseBody JSON 응답 문자열
     * @return video UUID
     */
    private String extractVideoIdFromResponse(String responseBody) {
        try {
            // JSON 파싱을 위한 정규표현식 (개선된 버전)
            // "videoId" 또는 "id" 필드를 찾음
            def videoIdPattern = /"(?:videoId|id)"\s*:\s*"([a-fA-F0-9-]{36})"/
            def matcher = responseBody =~ videoIdPattern
            
            if (matcher.find()) {
                String videoId = matcher.group(1)
                grinder.logger.info("Extracted videoId: ${videoId}")
                return videoId
                
            } else {
                // 대체 패턴 시도 (UUID만 추출)
                def uuidPattern = /([a-fA-F0-9-]{36})/
                def uuidMatcher = responseBody =~ uuidPattern
                
                if (uuidMatcher.find()) {
                    String videoId = uuidMatcher.group(1)
                    grinder.logger.warn("Extracted videoId using fallback pattern: ${videoId}")
                    return videoId
                } else {
                    throw new RuntimeException("videoId not found in response")
                }
            }
            
        } catch (Exception e) {
            grinder.logger.error("Video ID extraction failed: ${e.message}")
            grinder.logger.error("Response body: ${responseBody}")
            throw new RuntimeException("Failed to extract video ID: ${e.message}")
        }
    }
    
    /**
     * 오디오 URL에서 실제 스트리밍 URL 추출
     * S3 직접 접근 URL이 포함된 응답에서 URL 추출
     * 
     * @param audioApiResponse 오디오 API 응답
     * @return 실제 스트리밍 URL
     */
    String extractAudioStreamingUrl(String audioApiResponse) {
        try {
            // JSON 응답에서 audioUrl 또는 url 필드 추출
            def urlPattern = /"(?:audioUrl|url)"\s*:\s*"([^"]+)"/
            def matcher = audioApiResponse =~ urlPattern
            
            if (matcher.find()) {
                String streamingUrl = matcher.group(1)
                grinder.logger.info("Extracted audio streaming URL: ${streamingUrl}")
                return streamingUrl
                
            } else {
                // 직접 URL인 경우 (API 응답이 단순 URL 문자열)
                if (audioApiResponse.startsWith("http")) {
                    grinder.logger.info("Using direct audio URL: ${audioApiResponse}")
                    return audioApiResponse.strip()
                } else {
                    throw new RuntimeException("Audio URL not found in response")
                }
            }
            
        } catch (Exception e) {
            grinder.logger.error("Audio URL extraction failed: ${e.message}")
            throw new RuntimeException("Failed to extract audio URL: ${e.message}")
        }
    }
}
