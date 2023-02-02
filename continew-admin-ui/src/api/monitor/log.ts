import axios from 'axios';
import qs from 'query-string';

const BASE_URL = '/monitor/log';

export interface LogRecord {
  logId: string;
  clientIp: string;
  location: string;
  browser: string;
  createTime: string;
}

export interface LoginLogRecord extends LogRecord {
  description: string;
  status: number;
  errorMsg: string;
  createUserString: string;
}

export interface OperationLogRecord extends LogRecord {
  module: string;
  description: string;
  status: number;
  errorMsg: string;
  createUserString: string;
}

export interface SystemLogRecord extends LogRecord {
  statusCode: number;
  requestMethod: string;
  requestUrl: string;
  elapsedTime: number;
  exceptionDetail?: string;
}

export interface SystemLogDetailRecord extends SystemLogRecord {
  requestHeaders: string;
  requestBody: string;
  responseHeaders: string;
  responseBody: string;
}

export interface LoginLogParam extends Partial<LoginLogRecord> {
  page: number;
  size: number;
  sort: Array<string>;
}

export interface LoginLogListRes {
  list: LoginLogRecord[];
  total: number;
}

export function listLoginLogList(params: LoginLogParam) {
  return axios.get<LoginLogListRes>(`${BASE_URL}/login`, {
    params,
    paramsSerializer: (obj) => {
      return qs.stringify(obj);
    }
  });
}

export interface OperationLogParams extends Partial<OperationLogRecord> {
  page: number;
  size: number;
  sort: Array<string>;
  uid?: string;
}

export interface OperationLogListRes {
  list: OperationLogRecord[];
  total: number;
}

export function getOperationLogList(params: OperationLogParams) {
  return axios.get<OperationLogListRes>(`${BASE_URL}/operation`, {
    params,
    paramsSerializer: (obj) => {
      return qs.stringify(obj);
    }
  });
}

export interface SystemLogParams extends Partial<SystemLogRecord> {
  page: number;
  size: number;
  sort: Array<string>;
}

export interface SystemLogListRes {
  list: SystemLogRecord[];
  total: number;
}

export function getSystemLogList(params: SystemLogParams) {
  return axios.get<SystemLogListRes>(`${BASE_URL}/system`, {
    params,
    paramsSerializer: (obj) => {
      return qs.stringify(obj);
    }
  });
}

export function getSystemLogDetail(logId: string) {
  return axios.get<SystemLogDetailRecord>(`${BASE_URL}/system/${logId}`);
}