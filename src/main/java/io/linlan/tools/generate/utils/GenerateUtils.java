package io.linlan.tools.generate.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import io.linlan.commons.core.CoreException;
import io.linlan.commons.core.DateUtils;

import io.linlan.tools.generate.entity.Column;
import io.linlan.tools.generate.entity.Table;

/**
 * 代码自动生产工具类
 *
 * @author <a href="mailto:20400301@qq.com">linlan</a>
 * CreateTime:2017-11-07 8:01 PM
 *
 * @version 1.0
 * @since 1.0
 *
 */
public class GenerateUtils {

	/**
	 * generate the code with input table,columns,output in zip file
	 * @param table the table need to generate
	 * @param columns the columns of table
	 * @param zip the output zip file
	 */
	public static void generatorCode(Map<String, String> table, List<Map<String, String>> columns, ZipOutputStream zip) {
		// 配置信息
		Configuration config = getConfig();

		// 表信息
		Table t = new Table();
		t.setTableName(table.get("tableName"));
		t.setComments(table.get("tableComment"));
		// 表名转换成Java类名
		String className = tableToJava(t.getTableName(), config.getString("tablePrefix"));
		t.setClassName(className);
		t.setClassname(StringUtils.uncapitalize(className));

		// 列信息
		List<Column> columnList = new ArrayList<Column>();
		for (Map<String, String> column : columns) {
			Column c = new Column();

			c.setColumnName(column.get("columnName"));
			c.setDataType(column.get("dataType"));
			c.setComments(column.get("columnComment"));
			c.setExtra(column.get("extra"));

			// 列名转换成Java属性名
			String attrName = null;
			// 是否主键，如果是主键，采用统一的Id
			// 增加Oracle的主键约束处理，在查询数据库时默认第一个字段为主键
			if (("PRI".equalsIgnoreCase(column.get("columnKey")) && t.getPk() == null) || (column.get("columnKey") == "1")) {
				t.setPk(c);
				attrName = "Id";
			} else {
				// 其他非主键，直接采用字段名称及规则
				attrName = columnToJava(c.getColumnName());
			}
			c.setAttrName(attrName);
			c.setAttrname(StringUtils.uncapitalize(attrName));

			// 列的数据类型，转换成Java类型
			String attrType = config.getString(c.getDataType(), "unknowType");
			c.setAttrType(attrType);

			columnList.add(c);
		}
		t.setColumns(columnList);

		// 没主键，则第一个字段为主键
		if (t.getPk() == null) {
			t.setPk(t.getColumns().get(0));
		}

		// 设置velocity资源加载器
		Properties prop = new Properties();
		prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		Velocity.init(prop);

		// 封装模板数据
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("tableName", t.getTableName());
		map.put("comments", t.getComments());
		map.put("pk", t.getPk());
		map.put("className", t.getClassName());
		map.put("classname", t.getClassname());
		map.put("columns", t.getColumns());
		// 按照每个类型的数据表进行单独导出处理,如admin/global/op等
		String classGroup = config.getString("classGroup");
		map.put("classGroup", classGroup);
		// package的名称加上classGroup
		String packageStr = config.getString("package") + "." + classGroup;
		map.put("package", packageStr);
		String packageSlash = packageStr.replace(".", "/");
		map.put("packageSlash", packageSlash);

		String classPrefix = config.getString("classPrefix");

		// linlan modify for cms,在同一个表前缀下，对业务进行细分是，action统一为cms开头
		// 增加classPrefix变量，并修改sname和classGroup的输出信息
		String sname = "";
		if (StringUtils.isNotBlank(classPrefix)) {
			// 如果配置了classPrefix，如cms,dashboard
			// CmsContent对象，放在content包内，前缀为cms，则action为cms/content，修改修改map.put("classGroup", classPrefix);
			// DashboardBoard对象，放在board包内，前缀为dashboard，则action为dashboard/board
			// ComPlug对象，comm目录，临时调整前缀为comm，取消的为com，修改map.put("classGroup", classGroup);
			if (!classGroup.equals(classPrefix)) {
				// 而且classPrefix不等于classGroup，
				// 则Controller的action组成规则变为classPrefix/classGroup
				// 对于SA工程下的cms，mms等内容，需要采用统一的cms前缀，修改classGroup变量
				// 如表名称为cms_content，此时classGroup设置为content，classPrefix为cms
				// 则action为cms/content
				map.put("classGroup", classPrefix);
				// map.put("classGroup", classGroup);
				sname = t.getClassname().toLowerCase().replace(classPrefix, "");
				// sname = t.getClassname().toLowerCase().replace(classGroup,"");

			} else {
				sname = t.getClassname().toLowerCase().replace(classGroup, "");
			}
		} else {
			// 如果没有配置classPrefix，则表示采用通用模式进行代码生成
			// 将sname截取只取用去除classGroup的剩余部分信息
			// 如表名称为admin_user，此时classGroup设置为admin，则sname为user
			sname = t.getClassname().toLowerCase().replace(classGroup, "");
		}
		map.put("sname", sname);
		// 将APP_ID输出
		map.put("defAppId", config.getString("def_app_id"));
		// 增加后台管理目录路径设置
		String adminRoot = config.getString("adminRoot");
		map.put("adminRoot", adminRoot);
		map.put("author", config.getString("author"));
		map.put("email", config.getString("email"));
		map.put("datetime", DateUtils.format(new Date(), DateUtils.yyyyMMddHHmmss));
		VelocityContext context = new VelocityContext(map);

		// 获取模板列表
		List<String> templates = new ArrayList<String>();
		// 处理公共模板，java目录下为公共模板,template/java/
		String tplPathJavaModel = config.getString("tpl_path_model");
		if ("true".equals(config.getString("is_entity"))) {
			templates.add(tplPathJavaModel + "Entity.java.vm");
		}
		if ("true".equals(config.getString("is_dao"))) {
			templates.add(tplPathJavaModel + "Dao.java.vm");
		}
		if ("true".equals(config.getString("is_service"))) {
			templates.add(tplPathJavaModel + "Service.java.vm");
		}
		if ("true".equals(config.getString("is_dto"))) {
			templates.add(tplPathJavaModel + "Dto.java.vm");
		}
		if ("true".equals(config.getString("is_dto"))) {
			templates.add(tplPathJavaModel + "Param.java.vm");
		}

		// TODO 增加View业务层模板文件
		String viewPackage = config.getString("viewPackage");
		String viewPackageStr = viewPackage + "." + classGroup;
		map.put("viewPackage", viewPackageStr);
		String tplPathJavaView = config.getString("tpl_path_view");
		if ("true".equals(config.getString("is_member"))) {
			templates.add(tplPathJavaView + "EntryManager.java.vm");
			templates.add(tplPathJavaView + "OpManager.java.vm");
		}
//		if ("true".equals(config.getString("is_pub"))) {
//			templates.add(tplPathJavaView + "PubMemberService.java.vm");
//			templates.add(tplPathJavaView + "PubAdminService.java.vm");
//		}
//		if ("true".equals(config.getString("is_plat"))) {
//			templates.add(tplPathJavaView + "PlatMemberService.java.vm");
//			templates.add(tplPathJavaView + "PlatAdminService.java.vm");
//		}
		if ("true".equals(config.getString("is_vo"))) {
			templates.add(tplPathJavaView + "Vo.java.vm");
		}

		// TODO 增加Controller控制层模板文件
		String controllerPackage = config.getString("controllerPackage");
		String controllerPackageStr = controllerPackage + "." + classGroup;
		map.put("controllerPackage", controllerPackageStr);
		String tplPathJavaController = config.getString("tpl_path_controller");
		if ("true".equals(config.getString("is_member_api"))) {
			templates.add(tplPathJavaController + "EntryController.java.vm");
			templates.add(tplPathJavaController + "OpController.java.vm");
		}
//		if ("true".equals(config.getString("is_pub_api"))) {
//			templates.add(tplPathJavaController + "PubApiEntry.java.vm");
//			templates.add(tplPathJavaController + "PubApiOp.java.vm");
//		}

		// 处理不同数据库，生成不同的xml和sql
		String tplPathDb = config.getString("tpl_path_db");
		if ("true".equals(config.getString("is_dao"))) {
			templates.add(tplPathDb + "Dao.xml.vm");
		}

		// 处理admin-lte模式的后台页面文件，后续不在使用
//		String tplPathLte = config.getString("tpl_path_lte");
//		if ("true".equals(config.getString("is_page_lte"))) {
//			templates.add(tplPathLte + "list.html.vm");
//			templates.add(tplPathLte + "list.js.vm");
//		}

		// TODO 处理admin-node模式的前台页面文件
//		String tplPathNode = config.getString("tpl_path_node");
//		if ("true".equals(config.getString("is_page_node"))) {
//			templates.add(tplPathNode + "add-or-edit.vue.vm");
//			templates.add(tplPathNode + "index.js.vm");
//			templates.add(tplPathNode + "index.vue.vm");
//		}

		for (String template : templates) {
			// 渲染模板
			StringWriter sw = new StringWriter();
			Template tpl = Velocity.getTemplate(template, "UTF-8");
			tpl.merge(context, sw);

			try {
				// 添加到zip
				zip.putNextEntry(new ZipEntry(
						getFileName(template, t.getClassName(), packageStr, viewPackageStr, controllerPackageStr, adminRoot, sname)
				));
				IOUtils.write(sw.toString(), zip, "UTF-8");
				IOUtils.closeQuietly(sw);
				zip.closeEntry();
			} catch (IOException e) {
				throw new CoreException("渲染模板失败，表名：" + t.getTableName(), e);
			}
		}
	}

	public static void generatorXmlConfoundCode(Map<String, String> table, List<Map<String, String>> columns, ZipOutputStream zip) {
		table.get("tabValue");
		table.get("tableName");
		List<String> list = new ArrayList<String>();
		String dirName = "D:\\xml";
		try (Stream<Path> paths = Files.walk(Paths.get(dirName))) {
			paths.map(path -> path.toString()).filter(f -> f.endsWith(".xml")).forEach((e) -> {
				list.add(e);
			});
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// 配置信息
		Configuration config = getConfig();
		// 表名转换成Java类名
		String className = tableToJava(table.get("tableName"), config.getString("tablePrefix"));

		try {
			for (String l : list) {
				File file = new File(l);
				if (file.getName().equals(className + "Dao.xml")) {
					FileReader fileReader = new FileReader(file.getPath());
					char[] data = new char[1024];
					StringBuilder sb = new StringBuilder();
					int rn = 0;
					while ((rn = fileReader.read(data)) > 0) {
						String str = String.valueOf(data, 0, rn);
						sb.append(str);
					}

					String str = sb.toString().replace(table.get("tableName").toLowerCase(), table.get("tabValue"));
					str = str.replace(table.get("tableName").toUpperCase(), table.get("tabValue"));

					FileWriter fileWriter = new FileWriter(file.getPath());
					fileWriter.write(str.toCharArray());
					fileReader.close();
					fileWriter.close();
				}

			}

			zip.close();
		} catch (IOException e) {
			throw new CoreException("渲染模板失败，表名：" + table.get("tableName"), e);
		}

	}

	/**
	 * get the column with java pattern
	 * 列名转换成Java属性名，第一个字母大写，删除_
	 * @param columnName column name
	 * @return column name string
	 */
	public static String columnToJava(String columnName) {
		return WordUtils.capitalizeFully(columnName, new char[] {'_'}).replace("_", "");
	}

	/**
	 * get the tablename with java pattern
	 * 表名转换成Java类名
	 * @param tableName the input table name
	 * @param tablePrefix the prefix of table name
	 * @return table name in java entity
	 */
	public static String tableToJava(String tableName, String tablePrefix) {
		if (StringUtils.isNotBlank(tablePrefix)) {
			tableName = tableName.replace(tablePrefix, "");
		}
		return columnToJava(tableName);
	}

	/**
	 * get the config from properties file
	 * 获取配置信息
	 * @return {@link Configuration}
	 */
	public static Configuration getConfig() {
		try {
			FileBasedConfiguration config = new PropertiesConfiguration();

			FileHandler handler = new FileHandler(config);
			handler.setBasePath("/");
			handler.setFileName("generator.properties");
			handler.load();
			return config;
			// return new PropertiesConfiguration().("generator.properties");
		} catch (ConfigurationException e) {
			throw new CoreException("获取配置文件失败，", e);
		}
	}

	/**
	 * get the filename with tpl, className, package name, admin root path, short name
	 * 获取文件名
	 * @param template tpl1,tpl2 to generate
	 *        tpl1 for spring boot vue frame
	 *        tpl2 for spring mvc angular frame
	 * @param className the className to name java entity
	 * @param packageName the package to save java entity, add sname
	 * @param viewPackage the package to save view service and vo
	 * @param controllerPackage the package to save api controller
	 * @param adminRoot the path to save html, js
	 * @param sname the action prefix
	 * @return the filename string
	 */
	public static String getFileName(String template, String className, String packageName, String viewPackage, String controllerPackage, String adminRoot, String sname) {
		String packageBslash = "main" + File.separator + "java" + File.separator;
		String modelPackageBslash = packageBslash + "";
		String path = packageName.replace(".", File.separator) + File.separator;
		if (StringUtils.isNotBlank(packageName)) {
			modelPackageBslash += path;
		}
		//Model文件
		if (template.contains("model/Entity.java.vm")) {
			return modelPackageBslash + "entity" + File.separator + className + ".java";
		}

		if (template.contains("model/Dao.java.vm")) {
			return modelPackageBslash + "dao" + File.separator + className + "Dao.java";
		}
		if (template.contains("model/Dto.java.vm")) {
			return modelPackageBslash + "dto" + File.separator + className + "Dto.java";
		}
		if (template.contains("model/Param.java.vm")) {
			return modelPackageBslash + "param" + File.separator + className + "Param.java";
		}
		if (template.contains("model/Service.java.vm")) {
			return modelPackageBslash + "service" + File.separator + className + "Service.java";
		}
		// resources内的Mapper文件
		if (template.contains("Dao.xml.vm")) {
			return "main" + File.separator + "resources" + File.separator + "mapper" + File.separator + path + className + "Dao.xml";
		}

		//View文件 TODO
		String viewPackageBslash = packageBslash + "";
		String viewPackagePath = viewPackage.replace(".", File.separator) + File.separator;
		if (StringUtils.isNotBlank(viewPackagePath)) {
			viewPackageBslash += viewPackagePath;
		}
		if (template.contains("view/EntryManager.java.vm")) {
			return viewPackageBslash + "manager" + File.separator + className + "EntryManager.java";
		}
		if (template.contains("view/OpManager.java.vm")) {
			return viewPackageBslash + "manager" + File.separator + className + "OpManager.java";
		}
		if (template.contains("view/Vo.java.vm")) {
			return viewPackageBslash + "vo" + File.separator + className + "Vo.java";
		}
		//Controller文件 TODO
		String controllerPackageBslash = packageBslash + "";
		String controllerPath = controllerPackage.replace(".", File.separator) + File.separator;
		if (StringUtils.isNotBlank(controllerPackage)) {
			controllerPackageBslash += controllerPath;
		}
		if (template.contains("controller/EntryController.java.vm")) {
			return controllerPackageBslash + className + "EntryController.java";
		}
		if (template.contains("controller/OpController.java.vm")) {
			return controllerPackageBslash + className + "OpController.java";
		}

		// 根目录下的SQL文件
//		if (template.contains("menu.sql.vm")) {
//			return className.toLowerCase() + ".menu.sql";
//		}

//		// resources内的AdminLte后台文件
//		if (template.contains("list.html.vm")) {
//			return "main" + File.separator + "resources" + File.separator + "admin" + File.separator + adminRoot + File.separator + path + sname.toLowerCase() + ".html";
//		}
//
//		if (template.contains("list.js.vm")) {
//			return "main" + File.separator + "resources" + File.separator + "admin" + File.separator + adminRoot + File.separator + path + sname.toLowerCase() + ".js";
//		}

//		// resources内的AdminNode前台文件
//		if (template.contains("index.vue.vm")) {
//			return "main" + File.separator + "resources" + File.separator + "admin" + File.separator + adminRoot + File.separator + "src" + File.separator + "views" + File.separator + path
//					+ sname.toLowerCase() + File.separator + "index.vue";
//		}
//
//		if (template.contains("add-or-edit.vue.vm")) {
//			return "main" + File.separator + "resources" + File.separator + "admin" + File.separator + adminRoot + File.separator + "src" + File.separator + "views" + File.separator + path
//					+ sname.toLowerCase() + File.separator + "add-or-edit.vue";
//		}
//
//		if (template.contains("index.js.vm")) {
//			return "main" + File.separator + "resources" + File.separator + "admin" + File.separator + adminRoot + File.separator + "src" + File.separator + "api" + File.separator + path
//					+ sname.toLowerCase() + ".js";
//		}

		return null;
	}
}
