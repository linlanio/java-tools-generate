# java-tools-generate

## Description
```
- JAVA代码工具类，根据模板生成默认代码块，采用Model、View和Controller架构分层设计
- 代码生成系统，可在线生成后台Model域的entity、xml、dao、service文件，View域的vo和manager文件，Controller域的api文件
- 可生成前台的vue、ts代码，减少重复的开发工作
- 代码生成系统支撑mysql数据库、Oracle数据库生成

```

## 版本历史

```
2023-07-22  1.0   进行POM内配置调整和精简，增加java-api-doc目录，进行文档验证
2023-06-18  1.0   创建版本，进行结构划分，打包验证及本地发布
```

## Technical Route
### Software Architecture
```
1. 基于Springboot框架
2. 后台采用AdminLte和Bootstrap组件进行支撑
3. 页面模板采用Velocity模板
4. 前台支撑vue3和ts，实现增删改查通用功能
```

### Installation
```
1. mvn clean install
2. mvn clean deploy
```

## Use Effect

## contact us
website: https://www.linlan.io
email: contact@linlan.io

