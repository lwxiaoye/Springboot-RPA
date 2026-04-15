package rpa.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * AI代码生成响应DTO
 */
public class AiCodeGenerationResponse {

    @Data
    @lombok.EqualsAndHashCode(callSuper = false)
    public static class BaseResult {
        private boolean success;
        private String error;

        public static BaseResult error(String message) {
            BaseResult result = new BaseResult();
            result.success = false;
            result.error = message;
            return result;
        }
    }

    @Data
    @lombok.EqualsAndHashCode(callSuper = false)
    public static class AnalysisResult extends BaseResult {
        private String title;
        private boolean tableDetected;
        private String tableSelector;
        private List<String> columns;
        private boolean companyInfoDetected;
        private Map<String, String> companyInfo;
        private String htmlSnippet;
    }

    @Data
    @lombok.EqualsAndHashCode(callSuper = false)
    public static class CodeGenerationResult extends BaseResult {
        private String code;
        private String explanation;
    }

    @Data
    @lombok.EqualsAndHashCode(callSuper = false)
    public static class ColumnDetectionResult extends BaseResult {
        private Map<String, String> columns;
        private List<String> columnList;
    }

    @Data
    @lombok.EqualsAndHashCode(callSuper = false)
    public static class TemplateList extends BaseResult {
        private Map<String, String> templates;
    }
}
