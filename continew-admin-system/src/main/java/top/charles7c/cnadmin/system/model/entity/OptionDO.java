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

package top.charles7c.cnadmin.system.model.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 系统参数表
 *
 * @author Bull-BCLS
 * @since 2023/8/26 19:20
 */
@Data
@TableName("sys_option")
public class OptionDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 参数名称
     */
    private String name;

    /**
     * 参数键
     */
    @TableId
    private String code;

    /**
     * 参数值
     */
    private String value;

    /**
     * 参数默认值
     */
    private String defaultValue;

    /**
     * 描述
     */
    private String description;

    /**
     * 修改人
     */
    private Long updateUser;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;
}