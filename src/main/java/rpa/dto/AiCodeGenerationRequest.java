package rpa.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * AI代码生成请求和响应DTO
 */
public class AiCodeGenerationRequest {

    @Data
    public static class AnalysisRequest {
        private String url;
        private String htmlContent;
        private Map<String, String> headers;
    }

    @Data
    public static class GenerationRequest {
        private String prompt;
        private String url;
        private String targetTable;
        private String scene;
        private String tableSelector;
        private List<String> columns;
        private List<String> processSteps;
        private Map<String, String> fieldMapping;
        private boolean autoDetectColumns;
    }

    @Data
    public static class ColumnDetectionRequest {
        private String url;
        private String tableSelector;
    }
}
