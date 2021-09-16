package cn.wx.proxy.verticle;

import cn.wx.proxy.constant.EventBusAddr;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;

/**
 * 配置
 *  - https://vertx.io/docs/vertx-config/java/
 *
 * @author wangxin
 */
public class ConfigVerticle extends AbstractVerticle {





  @Override
  public void start(Promise<Void> verticlePromise) {

    ConfigRetriever retriever = ConfigRetriever.create(
      vertx,
      new ConfigRetrieverOptions()
        .addStore(
          new ConfigStoreOptions()
            .setType("file")
            .setFormat("properties")
            .setConfig(new JsonObject().put("path", "config.properties"))
        )
        .setScanPeriod(2000)
    );

    retriever.listen(change -> {
      System.out.println("刷新配置" + change.getNewConfiguration());
      vertx.eventBus().publish(
        EventBusAddr.CONFIG,
        change.getNewConfiguration(),
        new DeliveryOptions()
          .setLocalOnly(true)
          .setCodecName("JSON")
      );
    });

    retriever.getConfig().onSuccess(config -> {
      vertx.eventBus().request(
        EventBusAddr.CONFIG,
        config,
        new DeliveryOptions()
          .setSendTimeout(3_000)
          .setLocalOnly(true)
          .setCodecName("JSON"),
        msg -> {

        }
      );

      verticlePromise.complete();
    });
  }

}
