package cn.wx.proxy.constant;

/**
 * socket 代理阶段
 *
 * @author wangxin
 */
public enum SocketProxyStage {
  /**
   * 握手阶段
   */
  Handshake,
  /**
   * 授权阶段
   */
  MethodAuth,
  /**
   * 命令阶段
   */
  Cmd,
  /**
   * 工作阶段
   */
  Work,

}
