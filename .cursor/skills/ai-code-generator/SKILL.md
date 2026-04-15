# RPA代码生成AI助手 Skill

## 概述
为RPA平台提供智能化的机器人代码生成能力，自动分析目标网站并生成采集、解析、加工、落库的完整代码。

## 能力范围

### 1. 智能采集分析
- 分析目标网页HTML结构
- 自动识别表格数据和字段
- 检测数据分页和动态加载
- 识别企业信息字段（企业名称、信用代码、税号等）

### 2. 代码模板生成
根据不同的数据采集目标，生成对应的机器人代码：

```
// 发票采集场景
@collect URL
@table_selector #invoice-table tbody tr
@columns #,号码,类型,状态,日期,不含税,税额,价税合计
@log 采集完成

// 订单采集场景
@collect URL
@table_selector .order-table tbody tr
@columns 订单号,商品,数量,单价,金额,下单时间
@log 采集完成

// 企业信息采集
@collect URL
@selector company_name=.company-name
@selector credit_code=#creditCode
@selector tax_no=#taxNo
@log 采集完成
```

### 3. 通用落库机器人
- **智能表结构识别**：自动创建表或使用现有表
- **字段智能映射**：将采集的字段映射到数据库表字段
- **多表支持**：发票表、订单表、客户表、自定义表
- **数据清洗**：自动格式化金额、日期等
- **批量插入优化**：支持批量落库

## 用户场景

### 场景1：采集发票数据
**用户操作**：
1. 在"机器人管理"页面点击"AI生成代码"
2. 输入目标URL：`http://localhost:8081/spider_target`
3. 选择目标表：`invoice_data`
4. 点击"生成代码"

**AI生成代码**：
```
// 智能采集 - 发票数据
@collect http://localhost:8081/spider_target
@table_selector #invoice-table tbody tr
@columns #,号码,类型,状态,日期,不含税,税额,价税合计
@store invoice_data
@log 采集落库完成
```

### 场景2：采集订单数据
**用户操作**：
1. 选择"订单表"作为目标表
2. AI分析网站结构后生成对应代码

**生成代码**：
```
@collect http://example.com/orders
@table_selector .order-list tr
@columns 订单号,商品名称,数量,单价,总价,订单时间
@store order_data
@log 订单采集完成
```

### 场景3：采集企业信息
**用户操作**：
1. 输入目标网站
2. AI自动识别企业信息字段位置

**生成代码**：
```
@collect http://example.com/company
@selector company_name=.company-info h1
@selector credit_code=.credit-code
@selector tax_no=.tax-id
@store company_info
@log 企业信息采集完成
```

## 技术实现

### 后端服务
创建 `AiCodeGenerationService`：
```java
@Service
public class AiCodeGenerationService {
    // 分析网页结构
    public CodeAnalysisResult analyzeWebPage(String url);

    // 生成采集代码
    public String generateCollectCode(WebPageAnalysis analysis);

    // 生成解析代码
    public String generateParseCode(TableStructure table);

    // 生成落库代码
    public String generateStoreCode(String targetTable);

    // 一键生成完整流程
    public String generateFullWorkflow(GenerationRequest request);
}
```

### 前端组件
- 添加"AI生成代码"按钮
- 弹出配置对话框
- 预览生成代码
- 编辑和保存

## 扩展性

### 自定义模板
用户可以在 `skills/` 目录下添加自己的采集模板：

```
skills/
├── invoice-collect.skill      # 发票采集模板
├── order-collect.skill        # 订单采集模板
├── product-collect.skill      # 商品采集模板
└── custom-table.skill         # 自定义表格模板
```

### 插件机制
支持扩展解析器：
- Playwright 页面分析器
- Selenium 自动化分析
- 静态HTML分析器
- API接口识别器

## 优势

1. **降低使用门槛**：用户无需编写代码
2. **提高效率**：几分钟完成一个采集机器人
3. **标准化**：统一的代码风格和最佳实践
4. **可维护性**：清晰的代码结构
5. **可扩展**：支持各种网站和数据表

## 配置示例

创建 `ai-code-generator.skill`：
```yaml
name: "AI代码生成器"
version: "1.0.0"
description: "智能生成RPA机器人代码"
capabilities:
  - web_analysis
  - table_detection
  - code_generation
  - field_mapping
templates:
  - invoice
  - order
  - product
  - custom
```

---

这个Skill文档定义了整个AI代码生成系统的设计。接下来我需要帮你实现：
1. 前端AI生成按钮
2. 通用落库机器人服务
3. 智能代码生成逻辑

是否继续？我可以先实现通用落库机器人的核心逻辑。
