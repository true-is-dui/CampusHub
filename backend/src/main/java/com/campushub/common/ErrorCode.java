package com.campushub.common;

import lombok.Getter;

/**
 * 业务错误码，集中管理，避免在代码里散落魔数。
 *
 * <p>取值与 {@code api_design.yaml} 中的错误码契约一致：错误码为 5 位整数，
 * 前 3 位与对应的 HTTP 状态码相同（例如 40001 对应 HTTP 400），后 2 位区分
 * 同一类下的具体场景。成功响应不使用本枚举，固定 code=0。
 *
 * <p>每个枚举项携带三项信息：
 * <ul>
 *   <li>{@code code} —— 写入响应体 {@code code} 字段的业务码；</li>
 *   <li>{@code httpStatus} —— 该错误对应的 HTTP 状态码，由全局异常处理器写入响应行；</li>
 *   <li>{@code defaultMessage} —— 缺省提示语，调用方未提供更具体说明时使用。</li>
 * </ul>
 */
@Getter
public enum ErrorCode {

    /** 请求参数错误：字段校验失败、缺失必填项、枚举值非法等。 */
    INVALID_PARAM(40001, 400, "请求参数错误"),

    /** 认证失败：未登录、Token 缺失、无效或已过期。 */
    UNAUTHENTICATED(40101, 401, "未登录或登录已过期"),

    /** 权限或账号状态不允许：越权操作他人资源，或账号状态不满足操作前提。 */
    FORBIDDEN(40301, 403, "无权限或账号状态不允许"),

    /** 资源不存在：访问的对象不存在或已被删除。 */
    NOT_FOUND(40401, 404, "资源不存在"),

    /** 业务状态冲突：重复操作，或当前业务状态不允许该操作。 */
    CONFLICT(40901, 409, "操作冲突，当前状态不允许此操作"),

    /** 系统内部异常：未预期的服务端错误，对外不暴露细节。 */
    INTERNAL_ERROR(50001, 500, "系统内部错误"),

    /** 第三方服务异常：依赖的外部服务（如支付网关）调用失败。 */
    EXTERNAL_SERVICE_ERROR(50201, 502, "第三方服务暂时不可用");

  /**
   * -- GETTER --
   * 写入响应体 code 字段的业务错误码。
   */
  private final int code;
  /**
   * -- GETTER --
   * 对应的 HTTP 状态码。
   */
  private final int httpStatus;
  /**
   * -- GETTER --
   * 缺省提示语；调用方可用更具体的 message 覆盖。
   */
  private final String defaultMessage;

    ErrorCode(int code, int httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

}
