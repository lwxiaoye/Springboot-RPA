package rpa.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * NLP文本处理服务
 * 
 * 功能说明：
 * - 文本情感分析
 * - 关键词提取
 * - 文本分类
 * - 实体识别（姓名、地点、机构等）
 * - 文本翻译
 * 
 * @author RPA System
 */
@Slf4j
@Service
public class NlpService {

    @Value("${rpa.ai.mode:local}")
    private String mode;

    @Value("${rpa.ai.baidu.translate.app-id:}")
    private String baiduAppId;

    @Value("${rpa.ai.baidu.translate.secret:}")
    private String baiduSecret;

    // 情感词典
    private static final Map<String, Integer> POSITIVE_WORDS = new HashMap<>();
    private static final Map<String, Integer> NEGATIVE_WORDS = new HashMap<>();

    static {
        // 正面情感词
        POSITIVE_WORDS.put("好", 1);
        POSITIVE_WORDS.put("优秀", 2);
        POSITIVE_WORDS.put("满意", 2);
        POSITIVE_WORDS.put("喜欢", 2);
        POSITIVE_WORDS.put("棒", 2);
        POSITIVE_WORDS.put("赞", 2);
        POSITIVE_WORDS.put("不错", 1);
        POSITIVE_WORDS.put("很好", 2);
        POSITIVE_WORDS.put("非常好", 3);
        POSITIVE_WORDS.put("完美", 3);
        POSITIVE_WORDS.put("感谢", 1);
        POSITIVE_WORDS.put("谢谢", 1);
        POSITIVE_WORDS.put("happy", 2);
        POSITIVE_WORDS.put("good", 1);
        POSITIVE_WORDS.put("excellent", 3);
        POSITIVE_WORDS.put("great", 2);
        
        // 负面情感词
        NEGATIVE_WORDS.put("差", -1);
        NEGATIVE_WORDS.put("坏", -2);
        NEGATIVE_WORDS.put("不满意", -2);
        NEGATIVE_WORDS.put("讨厌", -2);
        NEGATIVE_WORDS.put("糟糕", -2);
        NEGATIVE_WORDS.put("很差", -2);
        NEGATIVE_WORDS.put("非常差", -3);
        NEGATIVE_WORDS.put("垃圾", -3);
        NEGATIVE_WORDS.put("问题", -1);
        NEGATIVE_WORDS.put("错误", -2);
        NEGATIVE_WORDS.put("失败", -2);
        NEGATIVE_WORDS.put("sad", -2);
        NEGATIVE_WORDS.put("bad", -1);
        NEGATIVE_WORDS.put("terrible", -3);
        NEGATIVE_WORDS.put("awful", -3);
    }

    /**
     * 文本情感分析
     */
    public AiService.AiResult analyzeSentiment(AiService.AiRequest request) {
        AiService.AiResult result = new AiService.AiResult();
        
        try {
            String text = request.getParams() != null ? 
                (String) request.getParams().get("text") : "";
            
            if (text.isEmpty()) {
                result.setSuccess(false);
                result.setErrorMessage("文本内容为空");
                return result;
            }
            
            // 计算情感得分
            double sentimentScore = calculateSentiment(text);
            
            // 判断情感极性
            String sentiment;
            if (sentimentScore > 0.5) {
                sentiment = "positive";
            } else if (sentimentScore < -0.5) {
                sentiment = "negative";
            } else {
                sentiment = "neutral";
            }
            
            result.setSuccess(true);
            result.setServiceType(AiService.AiServiceType.NLP);
            
            Map<String, Object> data = new HashMap<>();
            data.put("sentiment", sentiment);
            data.put("score", sentimentScore);
            data.put("positive", sentimentScore > 0);
            data.put("confidence", Math.min(0.95, Math.abs(sentimentScore) + 0.3));
            result.setData(data);
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("情感分析失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * NLP处理入口
     */
    public AiService.AiResult process(AiService.AiRequest request) {
        String operation = request.getParams() != null ?
            (String) request.getParams().getOrDefault("operation", "sentiment") : "sentiment";
        
        switch (operation) {
            case "sentiment":
                return analyzeSentiment(request);
            case "extract":
                return extractEntities(request);
            case "classify":
                return classifyText(request);
            case "keyword":
                return extractKeywords(request);
            default:
                return analyzeSentiment(request);
        }
    }

    /**
     * 文本翻译
     */
    public AiService.AiResult translate(AiService.AiRequest request) {
        AiService.AiResult result = new AiService.AiResult();
        
        try {
            String text = request.getParams() != null ?
                (String) request.getParams().get("text") : "";
            String from = request.getParams() != null ?
                (String) request.getParams().getOrDefault("from", "auto") : "auto";
            String to = request.getParams() != null ?
                (String) request.getParams().getOrDefault("to", "zh") : "zh";
            
            if (text.isEmpty()) {
                result.setSuccess(false);
                result.setErrorMessage("翻译内容为空");
                return result;
            }
            
            String translatedText;
            
            if ("cloud".equals(mode) && !baiduAppId.isEmpty()) {
                translatedText = translateWithBaidu(text, from, to);
            } else {
                translatedText = translateWithLocal(text, from, to);
            }
            
            result.setSuccess(true);
            result.setText(translatedText);
            result.setServiceType(AiService.AiServiceType.TRANSLATE);
            
            Map<String, Object> data = new HashMap<>();
            data.put("original", text);
            data.put("translated", translatedText);
            data.put("from", from);
            data.put("to", to);
            result.setData(data);
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("翻译失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 实体提取
     */
    public AiService.AiResult extractEntities(AiService.AiRequest request) {
        AiService.AiResult result = new AiService.AiResult();
        
        try {
            String text = request.getParams() != null ?
                (String) request.getParams().get("text") : "";
            
            if (text.isEmpty()) {
                result.setSuccess(false);
                result.setErrorMessage("文本内容为空");
                return result;
            }
            
            // 简单的实体识别
            List<Map<String, String>> entities = new ArrayList<>();
            
            // 识别人名（简单模式）
            java.util.regex.Pattern namePattern = 
                java.util.regex.Pattern.compile("[\\u4e00-\\u9fa5]{2,4}(?:先生|女士|老师|经理|总监)");
            java.util.regex.Matcher nameMatcher = namePattern.matcher(text);
            while (nameMatcher.find()) {
                Map<String, String> entity = new HashMap<>();
                entity.put("type", "PERSON");
                entity.put("value", nameMatcher.group());
                entities.add(entity);
            }
            
            // 识别地点
            java.util.regex.Pattern locPattern = 
                java.util.regex.Pattern.compile("(?:在|位于|地址)[\\u4e00-\\u9fa5a-zA-Z0-9]+(?:省|市|区|县|街|路|道|号)");
            java.util.regex.Matcher locMatcher = locPattern.matcher(text);
            while (locMatcher.find()) {
                Map<String, String> entity = new HashMap<>();
                entity.put("type", "LOCATION");
                entity.put("value", locMatcher.group());
                entities.add(entity);
            }
            
            // 识别组织机构
            java.util.regex.Pattern orgPattern = 
                java.util.regex.Pattern.compile("[\\u4e00-\\u9fa5]+(?:公司|集团|企业|银行|医院|学校|酒店|餐厅)");
            java.util.regex.Matcher orgMatcher = orgPattern.matcher(text);
            while (orgMatcher.find()) {
                Map<String, String> entity = new HashMap<>();
                entity.put("type", "ORGANIZATION");
                entity.put("value", orgMatcher.group());
                entities.add(entity);
            }
            
            // 识别日期
            java.util.regex.Pattern datePattern = 
                java.util.regex.Pattern.compile("\\d{4}[年/-]\\d{1,2}[月/-]\\d{1,2}[日]?");
            java.util.regex.Matcher dateMatcher = datePattern.matcher(text);
            while (dateMatcher.find()) {
                Map<String, String> entity = new HashMap<>();
                entity.put("type", "DATE");
                entity.put("value", dateMatcher.group());
                entities.add(entity);
            }
            
            result.setSuccess(true);
            result.setServiceType(AiService.AiServiceType.NLP);
            
            Map<String, Object> data = new HashMap<>();
            data.put("entities", entities);
            data.put("count", entities.size());
            result.setData(data);
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("实体提取失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 文本分类
     */
    public AiService.AiResult classifyText(AiService.AiRequest request) {
        AiService.AiResult result = new AiService.AiResult();
        
        try {
            String text = request.getParams() != null ?
                (String) request.getParams().get("text") : "";
            
            if (text.isEmpty()) {
                result.setSuccess(false);
                result.setErrorMessage("文本内容为空");
                return result;
            }
            
            // 简单的关键词匹配分类
            String category = classifyByKeywords(text);
            double confidence = calculateCategoryConfidence(text, category);
            
            result.setSuccess(true);
            result.setServiceType(AiService.AiServiceType.NLP);
            
            Map<String, Object> data = new HashMap<>();
            data.put("category", category);
            data.put("confidence", confidence);
            result.setData(data);
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("文本分类失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 关键词提取
     */
    public AiService.AiResult extractKeywords(AiService.AiRequest request) {
        AiService.AiResult result = new AiService.AiResult();
        
        try {
            String text = request.getParams() != null ?
                (String) request.getParams().get("text") : "";
            int topN = request.getParams() != null ?
                (Integer) request.getParams().getOrDefault("topN", 10) : 10;
            
            if (text.isEmpty()) {
                result.setSuccess(false);
                result.setErrorMessage("文本内容为空");
                return result;
            }
            
            // 简单的词频统计提取关键词
            Map<String, Integer> wordFreq = new HashMap<>();
            
            // 分词（简单按标点和空格分割）
            String[] words = text.split("[\\s,，.。!?？、;；:：\"\"]+");
            
            for (String word : words) {
                word = word.trim();
                if (word.length() >= 2 && word.length() <= 10) {
                    // 过滤纯数字和英文
                    if (!word.matches("\\d+") && !word.matches("[a-zA-Z]+")) {
                        wordFreq.put(word, wordFreq.getOrDefault(word, 0) + 1);
                    }
                }
            }
            
            // 排序取TopN
            List<Map.Entry<String, Integer>> sorted = new ArrayList<>(wordFreq.entrySet());
            sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
            
            List<Map<String, Object>> keywords = new ArrayList<>();
            int count = 0;
            for (Map.Entry<String, Integer> entry : sorted) {
                if (count++ >= topN) break;
                Map<String, Object> kw = new HashMap<>();
                kw.put("word", entry.getKey());
                kw.put("freq", entry.getValue());
                kw.put("score", entry.getValue() / (double) text.length() * 1000);
                keywords.add(kw);
            }
            
            result.setSuccess(true);
            result.setServiceType(AiService.AiServiceType.NLP);
            
            Map<String, Object> data = new HashMap<>();
            data.put("keywords", keywords);
            data.put("count", keywords.size());
            result.setData(data);
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("关键词提取失败: " + e.getMessage());
        }
        
        return result;
    }

    // ==================== 辅助方法 ====================

    /**
     * 计算情感得分
     */
    private double calculateSentiment(String text) {
        double score = 0.0;
        int matchCount = 0;
        
        for (Map.Entry<String, Integer> entry : POSITIVE_WORDS.entrySet()) {
            if (text.contains(entry.getKey())) {
                score += entry.getValue();
                matchCount++;
            }
        }
        
        for (Map.Entry<String, Integer> entry : NEGATIVE_WORDS.entrySet()) {
            if (text.contains(entry.getKey())) {
                score += entry.getValue();
                matchCount++;
            }
        }
        
        if (matchCount == 0) {
            return 0.0;
        }
        
        // 归一化到 [-1, 1]
        return Math.max(-1.0, Math.min(1.0, score / matchCount));
    }

    /**
     * 分类文本
     */
    private String classifyByKeywords(String text) {
        Map<String, String[]> categoryKeywords = new HashMap<>();
        
        categoryKeywords.put("金融", new String[]{"银行", "贷款", "利率", "投资", "理财", "股票", "基金", "保险"});
        categoryKeywords.put("教育", new String[]{"学校", "学生", "老师", "课程", "考试", "学习", "教育", "培训"});
        categoryKeywords.put("医疗", new String[]{"医院", "医生", "药品", "治疗", "健康", "疾病", "检查", "手术"});
        categoryKeywords.put("电商", new String[]{"订单", "商品", "购物", "支付", "快递", "评价", "退货", "优惠"});
        categoryKeywords.put("旅游", new String[]{"酒店", "机票", "景点", "旅游", "行程", "签证", "导游", "门票"});
        categoryKeywords.put("餐饮", new String[]{"餐厅", "美食", "外卖", "菜单", "预订", "评价", "优惠券"});
        categoryKeywords.put("房产", new String[]{"买房", "租房", "房价", "户型", "装修", "中介", "物业"});
        
        int maxMatch = 0;
        String category = "其他";
        
        for (Map.Entry<String, String[]> entry : categoryKeywords.entrySet()) {
            int match = 0;
            for (String keyword : entry.getValue()) {
                if (text.contains(keyword)) {
                    match++;
                }
            }
            if (match > maxMatch) {
                maxMatch = match;
                category = entry.getKey();
            }
        }
        
        return category;
    }

    /**
     * 计算分类置信度
     */
    private double calculateCategoryConfidence(String text, String category) {
        // 简单的置信度计算
        return Math.min(0.95, 0.3 + (text.length() / 500.0));
    }

    /**
     * 本地翻译（模拟）
     */
    private String translateWithLocal(String text, String from, String to) {
        // 这里应该是本地翻译引擎的实现
        // 暂时返回原文
        log.info("使用本地翻译（未配置云端API）");
        
        Map<String, String> dict = new HashMap<>();
        dict.put("hello", "你好");
        dict.put("world", "世界");
        dict.put("thank", "谢谢");
        dict.put("you", "你");
        dict.put("good", "好");
        dict.put("morning", "早上好");
        
        String lower = text.toLowerCase();
        for (Map.Entry<String, String> entry : dict.entrySet()) {
            lower = lower.replace(entry.getKey(), entry.getValue());
        }
        
        if (lower.equals(text.toLowerCase())) {
            return "[模拟翻译] " + text;
        }
        return lower;
    }

    /**
     * 百度翻译API（需配置）
     */
    private String translateWithBaidu(String text, String from, String to) {
        // 这里应该调用百度翻译API
        // 需要在配置文件中设置 app-id 和 secret
        log.warn("百度翻译API未配置，返回模拟结果");
        return "[百度翻译] " + text;
    }
}