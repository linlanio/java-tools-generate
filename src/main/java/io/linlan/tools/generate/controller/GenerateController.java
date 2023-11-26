/**
 * Copyright 2015 the original author or Linlan authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.linlan.tools.generate.controller;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.linlan.commons.core.Rcode;
import io.linlan.commons.core.ResponseResult;
import io.linlan.commons.db.page.Pagination;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import io.linlan.tools.generate.service.GenerateService;

/**
 * 代码自动生产控制类
 *
 * @author <a href="mailto:20400301@qq.com">linlan</a>
 * CreateTime:2017-11-07 8:01 PM
 *
 * @version 1.0
 * @since 1.0
 *
 */
@Controller
@RequestMapping("/tools/mysql")
public class GenerateController {

	/**
	 * @author
	 * @param   params 传递的参数
	 * @return 返回结果对象
	 */
	@ResponseBody
	@RequestMapping("/list")
//	public ResponseResult<Pagination> pageList(@RequestParam Map<String, Object> params) {
//		return ResponseResult.ok().setResultData(generateService.getListByPage(params));
//	}
	public Rcode pageList(@RequestParam Map<String, Object> params) {
		return Rcode.ok().put("page", generateService.getListByPage(params));
	}

	/**
	 * the generate the code of select tables
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	@RequestMapping("/code")
	public void code(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String[] tableNames = new String[]{};
		String tables = request.getParameter("tables");
		tableNames = JSON.parseArray(tables).toArray(tableNames);

		byte[] data = generateService.generatorCode(tableNames);

		response.reset();
		response.setHeader("Content-Disposition", "attachment; filename=\"tpl.zip\"");
		response.addHeader("Content-Length", "" + data.length);
		response.setContentType("application/octet-stream; charset=UTF-8");

		IOUtils.write(data, response.getOutputStream());
	}

	@Autowired
	private GenerateService generateService;

}
