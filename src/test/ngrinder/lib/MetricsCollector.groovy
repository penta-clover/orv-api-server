package lib

import static net.grinder.script.Grinder.grinder

/**
 * 성능 메트릭 수집 및 분석 클래스
 * Phase 1 Day 2에서 구현된 메트릭 관리 클래스
 */
class MetricsCollector {
    private List<Map> responseMetrics = []
    private List<String> errorLogs = []
    private Map<String, List<Long>> apiResponseTimes = [:]
    private Map<String, Integer> apiCallCounts = [:]
    private Map<String, Integer> apiErrorCounts = [:]
    
    /**
     * API 응답 메트릭 기록
     * 
     * @param apiName API 이름 (예: "AUTH_TEST_CALLBACK", "VIDEO_UPLOAD")
     * @param responseTime 응답 시간 (밀리초)
     * @param statusCode HTTP 상태 코드
     */
    void recordResponse(String apiName, long responseTime, int statusCode) {
        // 전체 메트릭 저장
        responseMetrics.add([
            api: apiName,
            responseTime: responseTime,
            statusCode: statusCode,
            timestamp: System.currentTimeMillis(),
            success: statusCode < 400
        ])
        
        // API별 응답시간 저장
        if (!apiResponseTimes.containsKey(apiName)) {
            apiResponseTimes[apiName] = []
        }
        apiResponseTimes[apiName].add(responseTime)
        
        // API별 호출 횟수 카운트
        apiCallCounts[apiName] = (apiCallCounts[apiName] ?: 0) + 1
        
        // 에러 카운트
        if (statusCode >= 400) {
            apiErrorCounts[apiName] = (apiErrorCounts[apiName] ?: 0) + 1
        }
        
        // 성능 목표 초과 경고
        if (responseTime > TestConfig.MAX_RESPONSE_TIME) {
            grinder.logger.warn("${apiName} response time exceeded target: ${responseTime}ms > ${TestConfig.MAX_RESPONSE_TIME}ms")
        }
    }
    
    /**
     * 에러 로그 기록
     * 
     * @param apiName API 이름
     * @param errorMessage 에러 메시지
     */
    void logError(String apiName, String errorMessage) {
        String timestamp = new Date().format("yyyy-MM-dd HH:mm:ss")
        String errorLog = "${timestamp}: [${apiName}] ${errorMessage}"
        
        errorLogs.add(errorLog)
        grinder.logger.error(errorLog)
        
        // 에러율 체크
        double currentErrorRate = calculateErrorRate(apiName)
        if (currentErrorRate > TestConfig.MAX_ERROR_RATE) {
            grinder.logger.warn("${apiName} error rate exceeded target: ${currentErrorRate * 100}% > ${TestConfig.MAX_ERROR_RATE * 100}%")
        }
    }
    
    /**
     * 특정 API의 에러율 계산
     * 
     * @param apiName API 이름
     * @return 에러율 (0.0 ~ 1.0)
     */
    double calculateErrorRate(String apiName) {
        int totalCalls = apiCallCounts[apiName] ?: 0
        int errorCalls = apiErrorCounts[apiName] ?: 0
        
        return totalCalls > 0 ? (double) errorCalls / totalCalls : 0.0
    }
    
    /**
     * 특정 API의 평균 응답시간 계산
     * 
     * @param apiName API 이름
     * @return 평균 응답시간 (밀리초)
     */
    double calculateAverageResponseTime(String apiName) {
        List<Long> responseTimes = apiResponseTimes[apiName]
        
        if (!responseTimes || responseTimes.isEmpty()) {
            return 0.0
        }
        
        long sum = responseTimes.sum()
        return (double) sum / responseTimes.size()
    }
    
    /**
     * 특정 API의 최대 응답시간 계산
     * 
     * @param apiName API 이름
     * @return 최대 응답시간 (밀리초)
     */
    long calculateMaxResponseTime(String apiName) {
        List<Long> responseTimes = apiResponseTimes[apiName]
        return responseTimes ? responseTimes.max() : 0L
    }
    
    /**
     * 특정 API의 최소 응답시간 계산
     * 
     * @param apiName API 이름
     * @return 최소 응답시간 (밀리초)
     */
    long calculateMinResponseTime(String apiName) {
        List<Long> responseTimes = apiResponseTimes[apiName]
        return responseTimes ? responseTimes.min() : 0L
    }
    
    /**
     * 특정 API의 95 백분위수 응답시간 계산
     * 
     * @param apiName API 이름
     * @return 95 백분위수 응답시간 (밀리초)
     */
    long calculateP95ResponseTime(String apiName) {
        List<Long> responseTimes = apiResponseTimes[apiName]
        
        if (!responseTimes || responseTimes.isEmpty()) {
            return 0L
        }
        
        List<Long> sortedTimes = responseTimes.sort()
        int p95Index = (int) Math.ceil(sortedTimes.size() * 0.95) - 1
        return sortedTimes[Math.max(0, p95Index)]
    }
    
    /**
     * 전체 통계 정보 반환
     * 
     * @return 통계 정보 맵
     */
    Map getStatistics() {
        Map stats = [:]
        
        // 전체 메트릭
        int totalRequests = responseMetrics.size()
        int totalErrors = responseMetrics.count { it.statusCode >= 400 }
        double overallErrorRate = totalRequests > 0 ? (double) totalErrors / totalRequests : 0.0
        
        stats['overall'] = [
            totalRequests: totalRequests,
            totalErrors: totalErrors,
            errorRate: overallErrorRate,
            errorRatePercentage: String.format("%.2f%%", overallErrorRate * 100)
        ]
        
        // API별 상세 통계
        stats['byApi'] = [:]
        apiCallCounts.each { apiName, callCount ->
            stats['byApi'][apiName] = [
                totalCalls: callCount,
                errorCount: apiErrorCounts[apiName] ?: 0,
                errorRate: calculateErrorRate(apiName),
                averageResponseTime: calculateAverageResponseTime(apiName),
                minResponseTime: calculateMinResponseTime(apiName),
                maxResponseTime: calculateMaxResponseTime(apiName),
                p95ResponseTime: calculateP95ResponseTime(apiName)
            ]
        }
        
        return stats
    }
    
    /**
     * 실시간 성능 요약 출력
     */
    void printRealTimeSummary() {
        Map stats = getStatistics()
        
        grinder.logger.info("=== Real-time Performance Summary ===")
        grinder.logger.info("Total Requests: ${stats.overall.totalRequests}")
        grinder.logger.info("Total Errors: ${stats.overall.totalErrors}")
        grinder.logger.info("Overall Error Rate: ${stats.overall.errorRatePercentage}")
        
        stats.byApi.each { apiName, apiStats ->
            grinder.logger.info("${apiName}: ${apiStats.totalCalls} calls, " +
                "avg: ${String.format('%.0f', apiStats.averageResponseTime)}ms, " +
                "p95: ${apiStats.p95ResponseTime}ms, " +
                "errors: ${apiStats.errorCount}")
        }
        grinder.logger.info("=====================================")
    }
    
    /**
     * 최종 성능 리포트 출력
     */
    void printFinalReport() {
        Map stats = getStatistics()
        
        grinder.logger.info("========== FINAL PERFORMANCE REPORT ==========")
        grinder.logger.info("Test Duration: ${calculateTestDuration()} seconds")
        grinder.logger.info("Total Requests: ${stats.overall.totalRequests}")
        grinder.logger.info("Total Errors: ${stats.overall.totalErrors}")
        grinder.logger.info("Overall Error Rate: ${stats.overall.errorRatePercentage}")
        
        // 성능 목표 달성 여부
        boolean errorRateAchieved = stats.overall.errorRate <= TestConfig.MAX_ERROR_RATE
        grinder.logger.info("Error Rate Target (${TestConfig.MAX_ERROR_RATE * 100}%): ${errorRateAchieved ? 'ACHIEVED' : 'FAILED'}")
        
        // API별 상세 리포트
        grinder.logger.info("\n--- API Performance Details ---")
        stats.byApi.each { apiName, apiStats ->
            boolean responseTimeAchieved = apiStats.averageResponseTime <= TestConfig.MAX_RESPONSE_TIME
            boolean apiErrorRateAchieved = apiStats.errorRate <= TestConfig.MAX_ERROR_RATE
            
            grinder.logger.info("${apiName}:")
            grinder.logger.info("  Calls: ${apiStats.totalCalls}")
            grinder.logger.info("  Avg Response Time: ${String.format('%.2f', apiStats.averageResponseTime)}ms (Target: ${TestConfig.MAX_RESPONSE_TIME}ms) ${responseTimeAchieved ? '✓' : '✗'}")
            grinder.logger.info("  P95 Response Time: ${apiStats.p95ResponseTime}ms")
            grinder.logger.info("  Min/Max: ${apiStats.minResponseTime}ms / ${apiStats.maxResponseTime}ms")
            grinder.logger.info("  Error Rate: ${String.format('%.2f', apiStats.errorRate * 100)}% (Target: ${TestConfig.MAX_ERROR_RATE * 100}%) ${apiErrorRateAchieved ? '✓' : '✗'}")
        }
        
        // 에러 로그 요약
        if (!errorLogs.isEmpty()) {
            grinder.logger.info("\n--- Error Log Summary ---")
            grinder.logger.info("Total Error Messages: ${errorLogs.size()}")
            
            // 최근 5개 에러만 출력
            def recentErrors = errorLogs.size() > 5 ? errorLogs[-5..-1] : errorLogs
            recentErrors.each { error ->
                grinder.logger.info("  ${error}")
            }
        }
        
        grinder.logger.info("===============================================")
    }
    
    /**
     * CSV 형태로 메트릭 데이터 내보내기
     * 
     * @return CSV 문자열
     */
    String exportToCsv() {
        StringBuilder csv = new StringBuilder()
        csv.append("Timestamp,API,ResponseTime,StatusCode,Success\n")
        
        responseMetrics.each { metric ->
            csv.append("${metric.timestamp},${metric.api},${metric.responseTime},${metric.statusCode},${metric.success}\n")
        }
        
        return csv.toString()
    }
    
    /**
     * 테스트 지속 시간 계산
     * 
     * @return 테스트 지속 시간 (초)
     */
    private long calculateTestDuration() {
        if (responseMetrics.isEmpty()) {
            return 0
        }
        
        long startTime = responseMetrics.first().timestamp
        long endTime = responseMetrics.last().timestamp
        return (endTime - startTime) / 1000
    }
    
    /**
     * 메트릭 데이터 초기화
     */
    void reset() {
        responseMetrics.clear()
        errorLogs.clear()
        apiResponseTimes.clear()
        apiCallCounts.clear()
        apiErrorCounts.clear()
        
        grinder.logger.info("MetricsCollector has been reset")
    }
    
    /**
     * 현재 수집된 메트릭 개수 반환
     * 
     * @return 메트릭 개수
     */
    int getMetricsCount() {
        return responseMetrics.size()
    }
    
    /**
     * 특정 API의 성공률 계산
     * 
     * @param apiName API 이름
     * @return 성공률 (0.0 ~ 1.0)
     */
    double calculateSuccessRate(String apiName) {
        return 1.0 - calculateErrorRate(apiName)
    }
}
