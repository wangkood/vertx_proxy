package cn.wx.proxy.verticle;

import cn.wx.proxy.domain.ProxyProtocol;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectDecoder;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 代理自动选择
 *
 * @author wangxin
 */
@Slf4j
public class ProxyVerticle  extends AbstractVerticle {



  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    int port = 8899;
    List<ProxyProtocol> supper = new ArrayList<>();

    vertx.createNetServer()
      .connectHandler(socket -> {
        socket.handler(buff -> {
          byte firstByte = buff.getByte(0);
          if (firstByte == '4' && supper.contains(ProxyProtocol.Socket4)) {
            handlerSocket4Proxy(socket, buff);
          } else if (firstByte == '5' && supper.contains(ProxyProtocol.Socket5)) {
            handlerSocket5Proxy(socket, buff);
          } else if (
            buff.length() > 7 && "CONNECT".equals(new String(buff.getBytes(0, 7))) &&
              supper.contains(ProxyProtocol.Https)
          ) {
            handlerHttpsProxy(socket, buff);
          } else if (supper.contains(ProxyProtocol.Http)){
            handlerHttpProxy(socket, buff);
          } else {
            socket.end(Buffer.buffer("no supper"));
          }
        });
      })
      .listen(port)
      .onSuccess(server -> {
        log.info("");
        startPromise.complete();
      })
      .onFailure(startPromise::fail);
  }




  public void handlerSocket4Proxy(NetSocket socket, Buffer firstBuf) {
    socket.end(Buffer.buffer("no supper"));




  }

  public void handlerSocket5Proxy(NetSocket socket, Buffer firstBuf) {

  }

  public void handlerHttpProxy(NetSocket socket, Buffer firstBuf) {

    HttpRequestDecoder requestDecoder = new HttpRequestDecoder();



    socket.end(Buffer.buffer("no supper"));
  }

  public void handlerHttpsProxy(NetSocket socket, Buffer firstBuf) {

  }

  @Override
  public void stop(Promise<Void> stopPromise) throws Exception {
    super.stop(stopPromise);
  }
}
