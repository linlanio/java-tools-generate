package io.linlan.tools.generate.dao;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 代码自动生产视图类
 *
 * @author <a href="mailto:20400301@qq.com">linlan</a>
 * CreateTime:2017-11-07 8:01 PM
 *
 * @version 1.0
 * @since 1.0
 *
 */
@Repository
public interface GenerateDao {

    /** 通过查询条件获取列表对象，返回Map对象
     * @param map 查询条件map
     * @return 列表对象
     */
    List<Map<String, Object>> getList(Map<String, Object> map);

    /** 通过查询条件获取记录总数
     * @param map 查询条件map
     * @return 记录总数
     */
    int queryTotal(Map<String, Object> map);

    /** 查询单个表的基本信息，包含名称、存储引擎、表备注、创建时间等
     * @param tableName 英文表名称
     * @return 表基本信息map
     */
    Map<String, String> queryTable(String tableName);

    /** 查询单个表的字段信息，包含字段名称、字段类型、字段备注、是否主键等
     * @param tableName 英文表名称
     * @return 字段基本信息map
     */
    List<Map<String, String>> queryColumns(String tableName);
    
}
