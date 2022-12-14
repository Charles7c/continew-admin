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

package top.charles7c.cnadmin.monitor.interceptor;

import java.time.LocalDateTime;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONUtil;

import top.charles7c.cnadmin.common.model.dto.OperationLog;
import top.charles7c.cnadmin.common.util.IpUtils;
import top.charles7c.cnadmin.common.util.ServletUtils;
import top.charles7c.cnadmin.common.util.helper.LoginHelper;
import top.charles7c.cnadmin.common.util.holder.LogContextHolder;
import top.charles7c.cnadmin.monitor.annotation.Log;
import top.charles7c.cnadmin.monitor.config.properties.LogProperties;
import top.charles7c.cnadmin.monitor.enums.LogLevelEnum;
import top.charles7c.cnadmin.monitor.model.entity.SysLog;

/**
 * ?????????????????????
 *
 * @author Charles7c
 * @since 2022/12/24 21:14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogInterceptor implements HandlerInterceptor {

    private final LogProperties operationLogProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!checkIsNeedRecord(handler, request)) {
            return true;
        }

        // ??????????????????
        this.logCreateTime();
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception e) {
        // ?????????????????????????????????
        SysLog sysLog = this.logElapsedTimeAndException();
        if (sysLog == null) {
            return;
        }

        // ????????????
        this.logDescription(sysLog, handler);
        // ??????????????????
        this.logRequest(sysLog, request);
        // ??????????????????
        this.logResponse(sysLog, response);

        // ??????????????????
        SpringUtil.getApplicationContext().publishEvent(sysLog);
    }

    /**
     * ??????????????????
     */
    private void logCreateTime() {
        OperationLog operationLog = new OperationLog();
        operationLog.setCreateUser(LoginHelper.getUserId());
        operationLog.setCreateTime(LocalDateTime.now());
        LogContextHolder.set(operationLog);
    }

    /**
     * ?????????????????????????????????
     *
     * @return ????????????
     */
    private SysLog logElapsedTimeAndException() {
        OperationLog operationLog = LogContextHolder.get();
        if (operationLog != null) {
            LogContextHolder.remove();
            SysLog sysLog = new SysLog();
            sysLog.setCreateTime(operationLog.getCreateTime());
            sysLog.setElapsedTime(System.currentTimeMillis() - LocalDateTimeUtil.toEpochMilli(sysLog.getCreateTime()));
            sysLog.setLogLevel(LogLevelEnum.INFO);

            // ??????????????????
            Exception exception = operationLog.getException();
            if (exception != null) {
                sysLog.setLogLevel(LogLevelEnum.ERROR);
                sysLog.setException(ExceptionUtil.stacktraceToString(operationLog.getException(), -1));
            }
            return sysLog;
        }
        return null;
    }

    /**
     * ??????????????????
     *
     * @param sysLog
     *            ????????????
     * @param handler
     *            ?????????
     */
    private void logDescription(SysLog sysLog, Object handler) {
        HandlerMethod handlerMethod = (HandlerMethod)handler;
        Operation methodOperation = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Operation.class);
        Log methodLog = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Log.class);

        if (methodOperation != null) {
            sysLog.setDescription(
                StrUtil.isNotBlank(methodOperation.summary()) ? methodOperation.summary() : "????????????????????????????????????????????????");
        }
        // ?????????@Log("???????????????") -> ???????????????
        if (methodLog != null && StrUtil.isNotBlank(methodLog.value())) {
            sysLog.setDescription(methodLog.value());
        }
    }

    /**
     * ??????????????????
     *
     * @param sysLog
     *            ????????????
     * @param request
     *            ????????????
     */
    private void logRequest(SysLog sysLog, HttpServletRequest request) {
        sysLog.setRequestUrl(StrUtil.isBlank(request.getQueryString()) ? request.getRequestURL().toString()
            : request.getRequestURL().append("?").append(request.getQueryString()).toString());
        sysLog.setRequestMethod(request.getMethod());
        sysLog.setRequestHeader(this.desensitize(ServletUtil.getHeaderMap(request)));
        String requestBody = this.getRequestBody(request);
        if (StrUtil.isNotBlank(requestBody)) {
            sysLog.setRequestBody(this.desensitize(
                JSONUtil.isTypeJSON(requestBody) ? JSONUtil.parseObj(requestBody) : ServletUtil.getParamMap(request)));
        }
        sysLog.setRequestIp(ServletUtil.getClientIP(request));
        sysLog.setLocation(IpUtils.getCityInfo(sysLog.getRequestIp()));
        sysLog.setBrowser(ServletUtils.getBrowser(request));
        sysLog.setCreateUser(sysLog.getCreateUser() == null ? LoginHelper.getUserId() : sysLog.getCreateUser());
    }

    /**
     * ??????????????????
     *
     * @param sysLog
     *            ????????????
     * @param response
     *            ????????????
     */
    private void logResponse(SysLog sysLog, HttpServletResponse response) {
        sysLog.setStatusCode(response.getStatus());
        sysLog.setResponseHeader(this.desensitize(ServletUtil.getHeadersMap(response)));
        // ???????????????????????? JSON ???????????????
        String responseBody = this.getResponseBody(response);
        if (StrUtil.isNotBlank(responseBody) && JSONUtil.isTypeJSON(responseBody)) {
            sysLog.setResponseBody(responseBody);
        }
    }

    /**
     * ????????????
     *
     * @param waitDesensitizeData
     *            ???????????????
     * @return ???????????? JSON ???????????????
     */
    private String desensitize(Map waitDesensitizeData) {
        String desensitizeDataStr = JSONUtil.toJsonStr(waitDesensitizeData);
        try {
            if (CollUtil.isEmpty(waitDesensitizeData)) {
                return desensitizeDataStr;
            }

            for (String desensitizeProperty : operationLogProperties.getDesensitize()) {
                waitDesensitizeData.computeIfPresent(desensitizeProperty, (k, v) -> "****************");
                waitDesensitizeData.computeIfPresent(desensitizeProperty.toLowerCase(), (k, v) -> "****************");
                waitDesensitizeData.computeIfPresent(desensitizeProperty.toUpperCase(), (k, v) -> "****************");
            }
            return JSONUtil.toJsonStr(waitDesensitizeData);
        } catch (Exception ignored) {
        }
        return desensitizeDataStr;
    }

    /**
     * ???????????????
     *
     * @param request
     *            ????????????
     * @return ?????????
     */
    private String getRequestBody(HttpServletRequest request) {
        String requestBody = "";
        ContentCachingRequestWrapper wrapper = WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
        if (wrapper != null) {
            requestBody = StrUtil.utf8Str(wrapper.getContentAsByteArray());
        }
        return requestBody;
    }

    /**
     * ???????????????
     *
     * @param response
     *            ????????????
     * @return ?????????
     */
    private String getResponseBody(HttpServletResponse response) {
        String responseBody = "";
        ContentCachingResponseWrapper wrapper =
            WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
        if (wrapper != null) {
            responseBody = StrUtil.utf8Str(wrapper.getContentAsByteArray());
        }
        return responseBody;
    }

    /**
     * ?????????????????????????????????
     *
     * @param handler
     *            /
     * @param request
     *            /
     * @return true ???????????????false ???????????????
     */
    private boolean checkIsNeedRecord(Object handler, HttpServletRequest request) {
        // 1?????????????????????????????????????????????
        if (!(handler instanceof HandlerMethod) || Boolean.FALSE.equals(operationLogProperties.getEnabled())) {
            return false;
        }

        // 2???????????????????????????????????????
        HandlerMethod handlerMethod = (HandlerMethod)handler;
        Log methodLog = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Log.class);
        // 2.1 ????????????????????????????????????????????? @Log ?????????????????????????????????
        if (operationLogProperties.getExcludeMethods().contains(request.getMethod()) && methodLog == null) {
            return false;
        }
        // 2.2 ???????????????????????? @Log ?????????????????? @Operation ?????????????????????????????????
        Operation methodOperation = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Operation.class);
        if (methodLog == null && methodOperation == null) {
            return false;
        }
        // 2.3 ?????????????????????????????????????????????
        if (methodOperation != null && methodOperation.hidden()) {
            return false;
        }
        // 2.4 ?????????????????? @Log ???????????????????????????????????????????????????????????????
        return methodLog == null || !methodLog.ignore();
    }
}
