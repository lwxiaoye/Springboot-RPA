package rpa.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import rpa.entity.InvoiceData;
import rpa.repository.InvoiceDataRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 发票数据查询接口
 */
@RestController
@RequestMapping("/api/invoice")
@CrossOrigin(origins = "*")
public class InvoiceDataController {

    @Autowired
    private InvoiceDataRepository invoiceDataRepository;

    /**
     * 获取所有发票数据（按采集时间倒序，最近采集的排在前面）
     */
    @GetMapping
    public Map<String, Object> list() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<InvoiceData> list = invoiceDataRepository.findAllByOrderByCollectTimeDesc();
            result.put("code", 0);
            result.put("data", list);
            result.put("total", list.size());
        } catch (Exception e) {
            result.put("code", 1);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 根据ID获取发票详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            InvoiceData data = invoiceDataRepository.findById(id).orElse(null);
            if (data != null) {
                result.put("code", 0);
                result.put("data", data);
            } else {
                result.put("code", 1);
                result.put("message", "数据不存在");
            }
        } catch (Exception e) {
            result.put("code", 1);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 根据企业名称搜索（按采集时间倒序）
     */
    @GetMapping("/search/company")
    public Map<String, Object> searchByCompany(@RequestParam String name) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<InvoiceData> list = invoiceDataRepository.findByCompanyNameOrderByCollectTimeDesc(name);
            result.put("code", 0);
            result.put("data", list);
        } catch (Exception e) {
            result.put("code", 1);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 根据统一社会信用代码查询（按采集时间倒序）
     */
    @GetMapping("/search/creditCode")
    public Map<String, Object> searchByCreditCode(@RequestParam String creditCode) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<InvoiceData> list = invoiceDataRepository.findByCreditCodeOrderByCollectTimeDesc(creditCode);
            result.put("code", 0);
            result.put("data", list);
        } catch (Exception e) {
            result.put("code", 1);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 根据纳税人识别号查询（按采集时间倒序）
     */
    @GetMapping("/search/taxNo")
    public Map<String, Object> searchByTaxNo(@RequestParam String taxNo) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<InvoiceData> list = invoiceDataRepository.findByTaxNoOrderByCollectTimeDesc(taxNo);
            result.put("code", 0);
            result.put("data", list);
        } catch (Exception e) {
            result.put("code", 1);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取统计数据
     */
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 使用倒序查询获取最近采集的数据
            List<InvoiceData> all = invoiceDataRepository.findAllByOrderByCollectTimeDesc();
            long total = all.size();
            long normal = all.stream().filter(d -> "正常".equals(d.getInvoiceStatus())).count();
            long abnormal = all.stream().filter(d -> "作废".equals(d.getInvoiceStatus()) || "红冲".equals(d.getInvoiceStatus())).count();

            Map<String, Object> data = new HashMap<>();
            data.put("total", total);
            data.put("normal", normal);
            data.put("abnormal", abnormal);

            result.put("code", 0);
            result.put("data", data);
        } catch (Exception e) {
            result.put("code", 1);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
