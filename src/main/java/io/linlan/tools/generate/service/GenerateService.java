package io.linlan.tools.generate.service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import io.linlan.commons.db.query.Query;
import io.linlan.commons.db.page.Pagination;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.linlan.tools.generate.dao.GenerateDao;
import io.linlan.tools.generate.utils.GenerateUtils;

/**
 * 代码自动生产服务类
 *
 * @author <a href="mailto:20400301@qq.com">linlan</a>
 * CreateTime:2017-11-07 8:01 PM
 *
 * @version 1.0
 * @since 1.0
 *
 */
@Service
public class GenerateService {

	@Autowired
	private GenerateDao generateDao;

	/**
	 * @author 
	 * @param   params 查询提交
	 * @return Pagination 分页信息和数据
	 */
	public Pagination getListByPage(Map<String, Object> params) {
		// 查询条件转换
		Query query = new Query(params);
		// 根据查询条件查询列表数据
		List<Map<String, Object>> list = generateDao.getList(query);
		//根据查询条件计算总记录数
		int total = generateDao.queryTotal(params);
		//将查询结果信息和分页信息通过分页对象的构造方法进行填充对应的值
		Pagination pagination = new Pagination(list, total, query.getLimit(), query.getPage());
		return pagination;
	}

	/**
	 * query the total count by input select conditions
	 * 通过输入的条件查询记录总数
	 * @param map the input select conditions
	 * @return total count
	 */
	public int queryTotal(Map<String, Object> map) {
		return generateDao.queryTotal(map);
	}

	/**
	 * the all the table name of database schema with name like
	 * @param tableName the prefix of include str of name
	 * @return the tablenames in map
	 */
	public Map<String, String> queryTable(String tableName) {
		return generateDao.queryTable(tableName);
	}
	
//	public Map<String, String> queryTmpTable(String tableName) {
//		return generatorTmpDao.queryTmpTable(tableName);
//	}

	/**
	 * the all the table's column with tablename
	 * @param tableName the whole name of table
	 * @return the tablenames in map of list
	 */
	public List<Map<String, String>> queryColumns(String tableName) {
		return generateDao.queryColumns(tableName);
	}

	/**
	 * generate the code with input tablenames
	 * @param tableNames the input table names
	 * @return the stream of output in zip file
	 */
	public byte[] generatorCode(String[] tableNames) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ZipOutputStream zip = new ZipOutputStream(outputStream);

		for (String tableName : tableNames) {
			// 查询表信息
			Map<String, String> table = queryTable(tableName);
			// 查询列信息
			List<Map<String, String>> columns = queryColumns(tableName);
			// 生成代码
			GenerateUtils.generatorCode(table, columns, zip);
		}
		IOUtils.closeQuietly(zip);
		return outputStream.toByteArray();
	}

	
}
