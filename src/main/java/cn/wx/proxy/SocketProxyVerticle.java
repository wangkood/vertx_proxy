package cn.wx.proxy;

import cn.wx.proxy.constant.SocketParams;
import cn.wx.proxy.constant.SocketProxyStage;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import lombok.extern.log4j.Log4j2;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * socket4& socket5 代理服务
 *
 * @author wangxin
 */
@Log4j2
public class SocketProxyVerticle extends AbstractVerticle {

  private static NetClient netClient;

  private static final int port = 8070;


  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    netClient = vertx.createNetClient();

    vertx.createNetServer().
      connectHandler(socket ->
        socket.handler(buff -> {
          SocketProxyStage stage = vertx.getOrCreateContext().get(socket.writeHandlerID());
          if (stage == null) {
            handlerHandshake(vertx.getOrCreateContext(), socket, buff.getBytes());
          } else if (SocketProxyStage.Cmd.equals(stage)) {
            handlerRequest(vertx.getOrCreateContext(), socket, buff.getBytes());
          } else if (SocketProxyStage.Work.equals(stage)) {
            handlerWork(vertx.getOrCreateContext(), socket, buff.getBytes());
          }
        })
      )
      .listen(port)
      .onFailure(startPromise::fail)
      .onSuccess(s -> startPromise.complete());
  }

  private static void handlerWork(Context context, NetSocket socket, byte[] bytes) {
//    log.info("\r\n----------工作----------\r\n{}\r\n----------", Arrays.toString(bytes));

  }

  private static void handlerRequest(Context context, NetSocket socket, byte[] bytes) {
    // 解析socket版本
    SocketParams.VER version = SocketParams.VER.valueOfCode(bytes[0]);
    if (version == null) {
      socket.close();
      return;
    }

    // 解析cmd
    SocketParams.CMD cmd = SocketParams.CMD.valueOfCode(bytes[1]);

    // 解析地址
    SocketAddress addr = parseAddr(bytes);

    log.info("{} >> {}", cmd, addr);

    // 建立和远程服务器连接
    netClient.connect(addr)
      .onSuccess(remoteSocket -> {
        socket.handler(remoteSocket::write).closeHandler(v -> remoteSocket.close());
        remoteSocket.handler(socket::write).closeHandler(v -> socket.close());

        // 响应成功
        byte[] clone = bytes.clone();
        clone[1] = SocketParams.REP.Succeeded.byteCode();
        socket.write(
          Buffer.buffer().appendBytes(clone)
        );
        context.put(socket.writeHandlerID(), SocketProxyStage.Work);
      })
      .onFailure(t -> {
        log.error(t);
        // 响应失败
        byte[] clone = bytes.clone();
        clone[1] = SocketParams.REP.ConnectionRefused.byteCode();
        socket.write(
          Buffer.buffer().appendBytes(clone)
        );
        context.put(socket.writeHandlerID(), SocketProxyStage.Work);
      });


  }


  public static SocketAddress parseAddr(byte[] bytes) {
    // 解析ip和端口
    int portStartIdx;
    byte atypCode = bytes[3];
    StringBuilder ip = new StringBuilder();

    if (SocketParams.ATYP.IPv4.byteCode() == atypCode) {
      portStartIdx = 8;
      for (int idx = 4; idx < portStartIdx; idx++) {
        ip.append(bytes[idx] & 0xff).append(".");
      }
      ip.delete(ip.length() - 1, ip.length());
    } else if (SocketParams.ATYP.IPv6.byteCode() == atypCode) {
      //TODO
      portStartIdx = 20;
      for (int idx = 4; idx < portStartIdx; idx++) {
        ip.append(bytes[idx]).append(".");
      }
      ip.delete(ip.length() - 1, ip.length());
    } else if (SocketParams.ATYP.Host.byteCode() == atypCode) {
      portStartIdx = 5 + bytes[4];
      try {
        // TODO 编码集未知
        ip.append(new String(Arrays.copyOfRange(bytes, 5, portStartIdx), "GBK"));
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    } else {
      return null;
    }

    return SocketAddress.inetSocketAddress(
      ((bytes[portStartIdx] & 0xff) << 8) + (bytes[portStartIdx + 1] & 0xff)
      , ip.toString()
    );
  }


  /**
   * 处理握手
   *
   * @param context 存储值
   * @param socket  s
   * @param bytes   b
   */
  private static void handlerHandshake(Context context, NetSocket socket, byte[] bytes) {
    // 解析socket版本
    SocketParams.VER version = SocketParams.VER.valueOfCode(bytes[0]);
    if (version == null) {
      socket.close();
      return;
    }

    // 解析授权方式
    Set<SocketParams.METHOD> auths = parseAuthFunc(bytes);

    if (auths.contains(SocketParams.METHOD.None)) {
      socket.write(Buffer.buffer()
        .appendByte(version.byteCode())
        .appendByte(SocketParams.METHOD.None.byteCode()));
      context.put(socket.writeHandlerID(), SocketProxyStage.Cmd);
    } else if (auths.contains(SocketParams.METHOD.Passwd)) {
      socket.write(Buffer.buffer()
        .appendByte(version.byteCode())
        .appendByte(SocketParams.METHOD.Passwd.byteCode()));
      context.put(socket.writeHandlerID(), SocketProxyStage.Cmd);
    } else {
      socket.close();
    }
  }


  private static Set<SocketParams.METHOD> parseAuthFunc(byte[] bytes) {
    // 便利客户端支持的授权方式
    Set<SocketParams.METHOD> auths = new HashSet<>();
    for (int i = 2; i < Math.min(bytes.length, 2 + bytes[1]); i++) {
      byte authFunc = bytes[i];
      SocketParams.METHOD auth = SocketParams.METHOD.valueOfCode(bytes[i]);
      if (auth == null) {
        throw new RuntimeException("无法解析此鉴权方式 code=" + authFunc);
      }
      auths.add(auth);
    }
    return auths;
  }

}
