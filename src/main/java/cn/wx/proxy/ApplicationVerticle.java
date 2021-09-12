package cn.wx.proxy;

import io.vertx.core.AbstractVerticle;

import lombok.extern.log4j.Log4j2;

/**
 * main
 *
 * @author wangxin
 */
@Log4j2
public class ApplicationVerticle extends AbstractVerticle {

  @Override
  public void start() throws Exception {
    vertx.deployVerticle(new HttpProxyVerticle());
    vertx.deployVerticle(new SocketProxyVerticle());
  }
}
