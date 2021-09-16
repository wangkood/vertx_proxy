package cn.wx.proxy;

import io.vertx.core.Launcher;
import io.vertx.core.VertxOptions;

import java.util.Arrays;

public class AppLauncher extends Launcher {

  public static void main(String[] args) {
    System.out.println(Arrays.toString(args));
    new AppLauncher().dispatch(args);
  }


  @Override
  public void beforeStartingVertx(VertxOptions options) {
    super.beforeStartingVertx(
      options
        // 优先使用JNI实现的底层传输协议，提高性能
        .setPreferNativeTransport(true)
        // 设置阻塞线程超时
        .setBlockedThreadCheckInterval(500)
        .setWorkerPoolSize(20)
        .setMaxWorkerExecuteTime(5000)
        .setEventLoopPoolSize(8)
        .setMaxEventLoopExecuteTime(1000)
    );
  }
}
