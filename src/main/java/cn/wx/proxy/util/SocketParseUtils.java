package cn.wx.proxy.util;

import cn.wx.proxy.constant.SocketParams;
import io.vertx.core.net.SocketAddress;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 解析socket工具
 *
 * @author wangxin
 */
public class SocketParseUtils {

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
      System.out.println(bytes);
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

  public static SocketParams.VER parseVer(byte[] bytes) {
    return SocketParams.VER.valueOfCode(bytes[0]);
  }

  public static Set<SocketParams.METHOD> parseMethod(byte[] bytes) {
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
