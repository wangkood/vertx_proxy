package cn.wx.proxy;

import cn.wx.proxy.verticle.ConfigVerticle;
import io.vertx.core.AbstractVerticle;

import io.vertx.core.Future;
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
    vertx.deployVerticle(new ConfigVerticle());
    vertx.deployVerticle(new HttpProxyVerticle());
    vertx.deployVerticle(new SocketProxyVerticle());
  }
}
