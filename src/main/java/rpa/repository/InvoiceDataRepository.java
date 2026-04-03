package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rpa.entity.InvoiceData;
import java.util.List;

/**
 * 发票数据仓库接口
 * <p>
 * 提供发票数据的数据库操作方法。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Repository
public interface InvoiceDataRepository extends JpaRepository<InvoiceData, Long> {

    /**
     * 根据纳税人识别号查询发票数据
     */
    List<InvoiceData> findByTaxNo(String taxNo);

    /**
     * 根据统一社会信用代码查询发票数据
     */
    List<InvoiceData> findByCreditCode(String creditCode);

    /**
     * 根据企业名称查询发票数据
     */
    List<InvoiceData> findByCompanyName(String companyName);

    /**
     * 根据发票号码查询
     */
    InvoiceData findByInvoiceNo(String invoiceNo);

    /**
     * 根据采集任务ID查询
     */
    List<InvoiceData> findByCollectId(Long collectId);

    /**
     * 统计发票数量
     */
    long countByTaxNo(String taxNo);
}
