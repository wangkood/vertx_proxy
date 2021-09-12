package cn.wx.proxy;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.NetClient;
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

  private static NetClient netClient;
  private static HttpClient httpClient;

  @Override
  public void start(Promise<Void> promise) {
    netClient = vertx.createNetClient();
    httpClient = vertx.createHttpClient();

    vertx.createHttpServer()
      .requestHandler(request -> {
        if (request.method().equals(HttpMethod.CONNECT)) {
          handlerTunnelProxy(request);
        } else if (request.headers().contains("Proxy-Connection")) {
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
  private void handlerTunnelProxy(HttpServerRequest clientReq) {
    String[] hostSplit = clientReq.host().split(":");
    int remotePort = Integer.parseInt(hostSplit[1]);
    String remoteHost = hostSplit[0];

    // 建立和目标服务器之间的连接
    netClient.connect(remotePort, remoteHost)
      .onFailure(t -> {
        log.error("链接远端服务器失败", t);
        clientReq.connection().close();
      })
      .onSuccess(remoteSocket -> {
        // 建立客户和目标服务器之间隧道
        clientReq.toNetSocket()
          .onFailure(log::error)
          .onSuccess(clientSocket -> {
            clientSocket.handler(remoteSocket::write).closeHandler(v -> remoteSocket.close());
            remoteSocket.handler(clientSocket::write).closeHandler(v -> clientSocket.close());
            log.info("Tunnel {}:{} ----> {}:{}",
              clientSocket.remoteAddress().host(), clientSocket.remoteAddress().port(), remoteHost, remotePort);
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
            if (Objects.equals("Proxy-Connection", header.getKey())) {
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
