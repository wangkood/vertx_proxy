package cn.wx.proxy.util;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;

/**
 * 隧道工具
 *
 * @author wangxin
 */
public class TunnelUtils {

  private static void createPipe(ReadStream<Buffer> in, WriteStream<Buffer> out) {
    in.handler(buff -> {
      out.write(buff);
      if (out.writeQueueFull()) {
        in.pause();
      }
    });
    out.drainHandler(v -> in.resume());
  }

  /**
   * 创建隧道
   *
   * @param client 客户端
   * @param remote 远端
   */
  public static void createTunnel(NetSocket client, NetSocket remote) {
    createPipe(client, remote);
    createPipe(remote, client);

    client.closeHandler(v -> remote.close());
    remote.closeHandler(v -> client.close());
  }

}
