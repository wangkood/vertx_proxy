package cn.wx.proxy;

import cn.wx.proxy.constant.SocketParams;
import cn.wx.proxy.constant.SocketProxyStage;
import cn.wx.proxy.util.SocketParseUtils;
import cn.wx.proxy.util.TunnelUtils;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;
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
          SocketProxyStage stage = vertx.getOrCreateContext().get(socket.writeHandlerID()+ ":stage");

          if (stage == null) {
            handlerHandshake(vertx.getOrCreateContext(), socket, buff.getBytes());
          }
          else if (SocketProxyStage.MethodAuth.equals(stage)) {
            handlerMethodAuth(vertx.getOrCreateContext(), socket, buff);
          }
          else if (SocketProxyStage.Cmd.equals(stage)) {
            handlerCmd(vertx, socket, buff.getBytes());
          }
        })
      )
      .listen(port)
      .onFailure(startPromise::fail)
      .onSuccess(s -> startPromise.complete());
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
    SocketParams.VER version = SocketParseUtils.parseVer(bytes);
    if (version == null) {
      socket.close();
      return;
    }

    // 解析授权方式
    Set<SocketParams.METHOD> auths = SocketParseUtils.parseMethod(bytes);
    if (auths.contains(SocketParams.METHOD.None)) {
      socket.write(Buffer.buffer()
        .appendByte(version.byteCode())
        .appendByte(SocketParams.METHOD.None.byteCode()));
      context.put(socket.writeHandlerID()+ ":stage", SocketProxyStage.Cmd);
    } else if (auths.contains(SocketParams.METHOD.Passwd)) {
      socket.write(Buffer.buffer()
        .appendByte(version.byteCode())
        .appendByte(SocketParams.METHOD.Passwd.byteCode()));
      context.put(socket.writeHandlerID()+ ":stage", SocketProxyStage.MethodAuth);
    } else {
      // 不支持其他登录方式
      socket.end(Buffer.buffer()
        .appendByte(version.byteCode())
        .appendByte(SocketParams.METHOD.NoAcceptable.byteCode()));
    }
  }


  /**
   * 处理客户端登录
   *
   * @param context c
   * @param socket s
   * @param buff b
   */
  private static void handlerMethodAuth(Context context, NetSocket socket, Buffer buff) {
    byte[] bytes = buff.getBytes();

    int usernameStartIdx = 2;
    int usernameLen = bytes[1];
    int passwordStartIdx = usernameStartIdx + usernameLen + 1;
    int passwordLen = bytes[passwordStartIdx - 1];

    String username = new String(Arrays.copyOfRange(bytes, usernameStartIdx, usernameStartIdx + usernameLen));
    String password = new String(Arrays.copyOfRange(bytes, passwordStartIdx, passwordStartIdx + passwordLen));

    log.info("{} {}:{}", "auth", username, password);

    context.put(socket.writeHandlerID() + ":stage", SocketProxyStage.Cmd);

    // 响应成功
    socket.write(
      Buffer.buffer()
        .appendByte(SocketParams.METHOD_PASSWD_VER)
        .appendByte(SocketParams.MethodPasswdStatus.Success.byteCode())
    );
  }


  /**
   * 处理客户端命令
   *
   * @param vertx c
   * @param socket s
   * @param bytes b
   */
  private static void handlerCmd(Vertx vertx, NetSocket socket, byte[] bytes) {

    // 解析socket版本
    SocketParams.VER version = SocketParams.VER.valueOfCode(bytes[0]);
    if (version == null) {
      socket.close();
      return;
    }

    // 解析cmd
    SocketParams.CMD cmd = SocketParams.CMD.valueOfCode(bytes[1]);

    // 解析地址
    SocketAddress addr = SocketParseUtils.parseAddr(bytes);

    log.info("{}(4.4) {} >> {}", cmd, socket.remoteAddress(), addr);

    // 建立和远程服务器连接
    netClient.connect(addr)
      .onSuccess(remoteSocket -> {
        // 建立隧道
        TunnelUtils.createTunnel(socket, remoteSocket);

        // 响应客户端成功
        byte[] clone = bytes.clone();
        clone[1] = SocketParams.REP.Succeeded.byteCode();
        socket.write(
          Buffer.buffer().appendBytes(clone)
        );
      })
      .onFailure(t -> {
        log.error(t);
        // 响应失败
        byte[] clone = bytes.clone();
        clone[1] = SocketParams.REP.ConnectionRefused.byteCode();
        socket.write(
          Buffer.buffer().appendBytes(clone)
        );
      });
  }




}
