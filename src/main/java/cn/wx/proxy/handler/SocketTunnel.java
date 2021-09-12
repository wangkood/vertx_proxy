package cn.wx.proxy.handler;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetSocket;
import lombok.extern.log4j.Log4j2;

/**
 * client >>>> proxy(own) >>>> remote
 *  - 每一个代理请求都会创建一个处理器
 *
 * @author wangxin
 */
@Log4j2
public class SocketTunnel {
  private Vertx vertx;
  private NetSocket clientSocket;
  private NetSocket remoteSocket;


  public SocketTunnel(Vertx vertx, NetSocket clientSocket, NetSocket remoteSocket) {
    this.vertx = vertx;
    this.clientSocket = clientSocket;
    this.remoteSocket = remoteSocket;
    init();
  }

  private void init() {
    this.clientSocket.closeHandler(v -> remoteSocket.close());
    this.remoteSocket.closeHandler(v -> clientSocket.close());

    // client >>> remote
    this.clientSocket.handler(buff -> {
      log.info("{} {} >> {}", "Send", clientSocket.remoteAddress(), remoteSocket.remoteAddress());
      remoteSocket.write(buff);
    });
    // remote >>> client
    this.remoteSocket.handler(buff -> {
      log.info("{} {} << {}", "Rev", clientSocket.remoteAddress(), remoteSocket.remoteAddress());
      clientSocket.write(buff);
    });
  }

}
