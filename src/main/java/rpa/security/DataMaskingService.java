package rpa.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据脱敏服务
 * 
 * 功能说明：
 * - 对敏感数据进行脱敏处理
 * - 支持多种脱敏规则：身份证、手机号、银行卡、邮箱等
 * - 支持自定义脱敏规则
 * - 符合金融行业数据安全规范
 * 
 * 脱敏规则：
 * - 身份证：显示前3后4，中间脱敏
 * - 手机号：显示前3后4，中间脱敏
 * - 银行卡：只显示后4位
 * - 邮箱：显示前缀3位+@+域名
 * - 姓名：只显示姓，名字脱敏
 * - 地址：显示省市区，详情脱敏
 * 
 * @author RPA System
 */
@Slf4j
@Service
public class DataMaskingService {

    // 脱敏类型枚举
    public enum MaskingType {
        ID_CARD,        // 身份证
        PHONE,          // 手机号
        BANK_CARD,      // 银行卡
        EMAIL,          // 邮箱
        NAME,           // 姓名
        ADDRESS,        // 地址
        PASSWORD,        // 密码
        AMOUNT,         // 金额
        CUSTOM          // 自定义
    }

    // 默认掩码字符
    private static final char MASK_CHAR = '*';
    private static final String MASK_STRING = "****";

    // 内置正则表达式
    private static final Map<MaskingType, Pattern> PATTERNS = new HashMap<>();
    
    static {
        // 身份证：15位或18位
        PATTERNS.put(MaskingType.ID_CARD, 
            Pattern.compile("\\b(\\d{3})\\d{11}(\\d{4})\\b|\\b(\\d{3})\\d{8}(\\d{3}[\\dXx])\\b"));
        
        // 手机号：1开头11位
        PATTERNS.put(MaskingType.PHONE, 
            Pattern.compile("\\b(1[3-9]\\d)\\d{4}(\\d{4})\\b"));
        
        // 银行卡：16-19位
        PATTERNS.put(MaskingType.BANK_CARD, 
            Pattern.compile("\\b(\\d{4})\\d{8,11}(\\d{4})\\b"));
        
        // 邮箱
        PATTERNS.put(MaskingType.EMAIL, 
            Pattern.compile("([a-zA-Z0-9]{1,3})[a-zA-Z0-9]*@([a-zA-Z0-9]+\\.[a-zA-Z0-9.]+)"));
    }

    /**
     * 通用脱敏方法
     */
    public String mask(String text, MaskingType type) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        try {
            switch (type) {
                case ID_CARD:
                    return maskIdCard(text);
                case PHONE:
                    return maskPhone(text);
                case BANK_CARD:
                    return maskBankCard(text);
                case EMAIL:
                    return maskEmail(text);
                case NAME:
                    return maskName(text);
                case ADDRESS:
                    return maskAddress(text);
                case PASSWORD:
                    return maskPassword(text);
                case AMOUNT:
                    return maskAmount(text);
                default:
                    return maskCustom(text, MASK_STRING);
            }
        } catch (Exception e) {
            log.error("数据脱敏失败: type={}", type, e);
            return text;
        }
    }

    /**
     * 身份证脱敏
     * 规则：显示前3后4，中间脱敏
     * 例：110101199001011234 → 110****1234
     */
    public String maskIdCard(String idCard) {
        if (idCard == null || idCard.isEmpty()) {
            return idCard;
        }
        
        // 清理格式
        String clean = idCard.replaceAll("[^0-9Xx]", "");
        
        if (clean.length() == 15) {
            // 15位：前2后1
            return clean.substring(0, 2) + "***" + clean.substring(clean.length() - 1);
        } else if (clean.length() == 18) {
            // 18位：前3后4
            return clean.substring(0, 3) + "****" + clean.substring(clean.length() - 4);
        }
        
        return clean;
    }

    /**
     * 手机号脱敏
     * 规则：显示前3后4
     * 例：13812345678 → 138****5678
     */
    public String maskPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return phone;
        }
        
        // 清理格式
        String clean = phone.replaceAll("[^0-9]", "");
        
        if (clean.length() == 11) {
            return clean.substring(0, 3) + "****" + clean.substring(7);
        }
        
        return clean;
    }

    /**
     * 银行卡脱敏
     * 规则：只显示后4位
     * 例：6222021234567890123 → ************90123
     */
    public String maskBankCard(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            return cardNumber;
        }
        
        // 清理格式
        String clean = cardNumber.replaceAll("[^0-9]", "");
        
        if (clean.length() >= 8) {
            int maskLength = clean.length() - 4;
            String masked = repeatMask(maskLength) + clean.substring(maskLength);
            return masked;
        }
        
        return repeatMask(clean.length());
    }

    /**
     * 邮箱脱敏
     * 规则：显示前缀前1后3+@+域名
     * 例：zhangsan@example.com → z***san@example.com
     */
    public String maskEmail(String email) {
        if (email == null || email.isEmpty() || !email.contains("@")) {
            return email;
        }
        
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return email;
        }
        
        String prefix = parts[0];
        String domain = parts[1];
        
        if (prefix.length() <= 2) {
            return repeatMask(prefix.length()) + "@" + domain;
        } else {
            return prefix.charAt(0) + repeatMask(prefix.length() - 2) + prefix.charAt(prefix.length() - 1) + "@" + domain;
        }
    }

    /**
     * 姓名脱敏
     * 规则：只显示姓，名字脱敏
     * 例：张三 → 张*；欧阳娜娜 → 欧***
     */
    public String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        
        // 处理英文名
        if (name.matches("[a-zA-Z\\s]+")) {
            String[] parts = name.split("\\s+");
            if (parts.length >= 2) {
                return parts[0].charAt(0) + repeatMask(parts[0].length() - 1) + " " + 
                       repeatMask(parts[1].length());
            }
            return name.charAt(0) + repeatMask(name.length() - 1);
        }
        
        // 中文姓名
        if (name.length() == 2) {
            return name.charAt(0) + "*";
        } else if (name.length() == 3) {
            // 复姓
            if (isDoubleSurname(name.substring(0, 2))) {
                return name.substring(0, 2) + repeatMask(1);
            }
            return name.charAt(0) + repeatMask(2);
        } else if (name.length() > 3) {
            return name.charAt(0) + repeatMask(name.length() - 1);
        }
        
        return repeatMask(name.length());
    }

    /**
     * 地址脱敏
     * 规则：只显示省市区
     * 例：北京市朝阳区XX街道XX号 → 北京市朝阳区***
     */
    public String maskAddress(String address) {
        if (address == null || address.isEmpty()) {
            return address;
        }
        
        // 移除详细门牌号
        String masked = address;
        
        // 移除数字门牌号
        masked = masked.replaceAll("\\d+号", "");
        masked = masked.replaceAll("\\d+楼", "");
        
        // 截断到区/县级别
        int lastIndex = masked.length();
        String[] separators = {"弄", "栋", "单元", "室", "街", "路", "道"};
        
        for (String sep : separators) {
            int idx = masked.lastIndexOf(sep);
            if (idx > 0 && idx < lastIndex) {
                lastIndex = idx;
            }
        }
        
        if (lastIndex < masked.length()) {
            masked = masked.substring(0, lastIndex);
        }
        
        // 替换剩余内容为脱敏符
        if (masked.length() > 12) {
            masked = masked.substring(0, 12) + "***";
        }
        
        return masked;
    }

    /**
     * 密码脱敏
     * 规则：全部脱敏
     */
    public String maskPassword(String password) {
        if (password == null || password.isEmpty()) {
            return password;
        }
        return repeatMask(Math.min(password.length(), 12));
    }

    /**
     * 金额脱敏
     * 规则：显示前N位，保留小数
     * 例：12345.67 → 1******.67
     */
    public String maskAmount(String amount) {
        if (amount == null || amount.isEmpty()) {
            return amount;
        }
        
        String[] parts = amount.split("\\.");
        String integerPart = parts[0];
        String decimalPart = parts.length > 1 ? "." + parts[1] : "";
        
        if (integerPart.length() <= 1) {
            return repeatMask(1) + decimalPart;
        }
        
        return integerPart.charAt(0) + repeatMask(integerPart.length() - 1) + decimalPart;
    }

    /**
     * 自定义脱敏
     */
    public String maskCustom(String text, String mask) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return mask != null && !mask.isEmpty() ? mask : MASK_STRING;
    }

    /**
     * 根据正则表达式脱敏
     */
    public String maskByPattern(String text, String regex, int showStart, int showEnd) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        try {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(text);
            
            StringBuffer result = new StringBuffer();
            while (matcher.find()) {
                String match = matcher.group();
                if (match.length() <= showStart + showEnd) {
                    matcher.appendReplacement(result, repeatMask(match.length()));
                } else {
                    String masked = match.substring(0, showStart) + 
                                   repeatMask(match.length() - showStart - showEnd) + 
                                   match.substring(match.length() - showEnd);
                    matcher.appendReplacement(result, masked);
                }
            }
            matcher.appendTail(result);
            
            return result.toString();
        } catch (Exception e) {
            log.error("正则脱敏失败: regex={}", regex, e);
            return text;
        }
    }

    /**
     * 批量脱敏
     */
    public Map<String, Object> maskBatch(Map<String, Object> data, List<String> fields, Map<String, MaskingType> typeMap) {
        Map<String, Object> result = new HashMap<>(data);
        
        for (String field : fields) {
            if (result.containsKey(field)) {
                Object value = result.get(field);
                if (value != null) {
                    MaskingType type = typeMap.getOrDefault(field, MaskingType.CUSTOM);
                    String masked = mask(value.toString(), type);
                    result.put(field, masked);
                }
            }
        }
        
        return result;
    }

    /**
     * 判断是否为复姓
     */
    private boolean isDoubleSurname(String prefix) {
        String[] doubleSurnames = {
            "欧阳", "司马", "上官", "诸葛", "慕容", "令狐", "公孙", "西门", "南宫", "东方",
            "司徒", "慕容", "完颜", "耶律", "沙陀", "呼延", "赫连", "拓跋", "尉迟", "百里"
        };
        
        for (String ds : doubleSurnames) {
            if (prefix.equals(ds)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 生成脱敏字符
     */
    private String repeatMask(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(MASK_CHAR);
        }
        return sb.toString();
    }

    /**
     * 获取支持的脱敏类型
     */
    public List<Map<String, String>> getSupportedTypes() {
        List<Map<String, String>> types = new ArrayList<>();
        
        types.add(Map.of("type", "ID_CARD", "name", "身份证", "example", "110***********1234"));
        types.add(Map.of("type", "PHONE", "name", "手机号", "example", "138****5678"));
        types.add(Map.of("type", "BANK_CARD", "name", "银行卡", "example", "************9012"));
        types.add(Map.of("type", "EMAIL", "name", "邮箱", "example", "z***n@example.com"));
        types.add(Map.of("type", "NAME", "name", "姓名", "example", "张*"));
        types.add(Map.of("type", "ADDRESS", "name", "地址", "example", "北京市朝阳区***"));
        types.add(Map.of("type", "PASSWORD", "name", "密码", "example", "******"));
        types.add(Map.of("type", "AMOUNT", "name", "金额", "example", "1*****.99"));
        
        return types;
    }
}