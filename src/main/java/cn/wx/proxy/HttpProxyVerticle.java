package cn.wx.proxy;

import cn.wx.proxy.constant.EventBusAddr;
import cn.wx.proxy.domain.AddProxy;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.SocketAddress;
import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.util.Objects;

/**
 * http & https 代理服务器
 *
 * @author wangxin
 */
@Log4j2
public class HttpProxyVerticle extends AbstractVerticle {

  private static final String PROXY_CONNECTION_HEADER = "Proxy-Connection";

  private static NetClient netClient;
  private static HttpClient httpClient;

  @Override
  public void start(Promise<Void> promise) {

    netClient = vertx.createNetClient();
    httpClient = vertx.createHttpClient();

    vertx.createHttpServer(new HttpServerOptions().setTcpNoDelay(true).setTcpFastOpen(true).setTcpQuickAck(true))
      .requestHandler(request -> {
        if (request.method().equals(HttpMethod.CONNECT)) {
          handlerHttpsProxy(request);
        } else if (request.headers().contains(PROXY_CONNECTION_HEADER)) {
          handlerHttpProxy(request);
        } else {
          request.response().end("HELLO, I`M PROXY SERVER");
        }
      })
      .listen(8090)
      .onFailure(promise::fail)
      .onSuccess(s -> promise.complete());
  }

  /**
   * 处理 https 隧道请求
   *
   * @param clientReq 请求
   */
  private void handlerHttpsProxy(HttpServerRequest clientReq) {

    // 建立和目标服务器之间的连接
    String[] urlSplit = clientReq.uri().split(":");
    netClient.connect(SocketAddress.inetSocketAddress(Integer.parseInt(urlSplit[1]), urlSplit[0]))
      .onFailure(t -> {
        clientReq.connection().close();
        log.error("连接远端服务器失败 url=" + clientReq.uri() + " msg=" + t.getMessage());
      })
      .onSuccess(remoteSocket -> {
        // 建立客户和目标服务器之间隧道
        clientReq.toNetSocket()
          .onFailure(log::error)
          .onSuccess(clientSocket -> {
            clientSocket.handler(remoteSocket::write).closeHandler(v -> remoteSocket.close());
            remoteSocket.handler(clientSocket::write).closeHandler(v -> clientSocket.close());
            log.info("Tunnel {}:{} ----> {}:{}",
              clientSocket.remoteAddress().host(),
              clientSocket.remoteAddress().port(),
              remoteSocket.remoteAddress().host(),
              remoteSocket.remoteAddress().port());
          });
      });
  }

  /**
   * 处理 http 代理请求
   *
   * @param clientReq 请求
   */
  private void handlerHttpProxy(HttpServerRequest clientReq) {
    clientReq.body().onSuccess(buff ->
      httpClient.request(clientReq.method(), clientReq.host(), clientReq.uri())
        .onFailure(log::error)
        .onSuccess(httpClientReq -> {
          // 复制请求头
          for (Map.Entry<String, String> header : clientReq.headers()) {
            if (Objects.equals(PROXY_CONNECTION_HEADER, header.getKey())) {
              httpClientReq.putHeader(HttpHeaders.CONNECTION, header.getValue());
            }
            httpClientReq.putHeader(header.getKey(), header.getValue());
          }
          // 复制请求体并发送
          httpClientReq.send(buff).onSuccess(remoteResp -> {
            // 复制远程服务器响应头
            for (Map.Entry<String, String> header : remoteResp.headers()) {
              clientReq.response().putHeader(header.getKey(), header.getValue());
            }
            remoteResp.body().onSuccess(respBuff -> {
              clientReq.response().setStatusCode(remoteResp.statusCode()).end(respBuff);
              log.info("Proxy {} ----> {}", clientReq.remoteAddress(), clientReq.uri());
            });
          });
        })
    );
  }

}
