/*
 * Copyright (c) 2022-present Charles7c Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package top.charles7c.cnadmin.webapi.controller.system;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import top.charles7c.cnadmin.common.annotation.CrudRequestMapping;
import top.charles7c.cnadmin.common.base.BaseController;
import top.charles7c.cnadmin.common.base.BaseRequest;
import top.charles7c.cnadmin.common.consts.Constants;
import top.charles7c.cnadmin.common.model.vo.R;
import top.charles7c.cnadmin.system.model.query.UserQuery;
import top.charles7c.cnadmin.system.model.request.UserRequest;
import top.charles7c.cnadmin.system.model.vo.UserDetailVO;
import top.charles7c.cnadmin.system.model.vo.UserVO;
import top.charles7c.cnadmin.system.service.UserService;

/**
 * 用户管理 API
 *
 * @author Charles7c
 * @since 2023/2/20 21:00
 */
@Tag(name = "用户管理 API")
@RestController
@CrudRequestMapping("/system/user")
public class UserController extends BaseController<UserService, UserVO, UserDetailVO, UserQuery, UserRequest> {

    @Override
    protected R<Long> add(@Validated(BaseRequest.Create.class) @RequestBody UserRequest request) {
        Long id = baseService.add(request);
        return R.ok(String.format("新增成功，请牢记默认密码：%s", Constants.DEFAULT_PASSWORD), id);
    }
}