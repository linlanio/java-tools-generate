package io.linlan.tools.generate.entity;

import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * 表数据
 */
public class Table {
    //表的名称
    private String tableName;
    //表的备注
    private String comments;
    //表的主键
    private Column pk;
    //表的列名(不包含主键)
    private List<Column> columns;

    //类名(第一个字母大写)，如：core_user => CoreUser
    private String className;
    //类名(第一个字母小写)，如：core_user => coreUser
    private String classname;

    public String getTableName() {
        return tableName;
    }
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    public String getComments() {
        //mysql 数据库内的备注后面增加了InnoDB的信息，屏蔽处理
        if (StringUtils.isNotBlank(comments)){
            if (comments.indexOf(";") > 0){
                return comments.substring(0, comments.indexOf(";"));
            }
        }
        return comments;
    }
    public void setComments(String comments) {
        this.comments = comments;
    }
    public Column getPk() {
        return pk;
    }
    public void setPk(Column pk) {
        this.pk = pk;
    }
    public List<Column> getColumns() {
        return columns;
    }
    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }
    public String getClassName() {
        return className;
    }
    public void setClassName(String className) {
        this.className = className;
    }
    public String getClassname() {
        return classname;
    }
    public void setClassname(String classname) {
        this.classname = classname;
    }
}
