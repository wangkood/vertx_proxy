package cn.wx.proxy.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;

/**
 * socket 协议常用参数
 *
 * @author wangxin
 */
public class SocketParams {


  public static final byte RSV = 0x00;

  @Getter
  @Accessors(fluent = true)
  @AllArgsConstructor
  public enum VER {
    /**
     * socket4
     */
    S4((byte) 0x04),
    /**
     * socket5
     */
    S5((byte) 0x05),
    ;
    final byte byteCode;

    public static VER valueOfCode(byte byteCode) {
      return Arrays.stream(values()).filter(v -> v.byteCode == byteCode).findFirst().orElse(null);
    }
  }

  @Getter
  @Accessors(fluent = true)
  @AllArgsConstructor
  public enum CMD {
    /**
     * 链接
     */
    Connect((byte) 0x01),
    /**
     * 端口监听， 在server上监听一个端口
     */
    Bind((byte) 0x02),
    /**
     * 使用UDP
     */
    UdpAssociate((byte) 0x03),
    ;
    final byte byteCode;

    public static CMD valueOfCode(byte byteCode) {
      return Arrays.stream(values()).filter(v -> v.byteCode == byteCode).findFirst().orElse(null);
    }
  }

  @Getter
  @Accessors(fluent = true)
  @AllArgsConstructor
  public enum METHOD {
    /**
     * 不需要认证
     */
    None((byte) 0x00),
    /**
     * GssApi认证
     */
    GssApi((byte) 0x01),
    /**
     * 用户名和密码认证
     */
    Passwd((byte) 0x03),
    /**
     * 保留作私有用处
     */
    ReservedForPrivate((byte) 0x80),
    /**
     * 不接受任何方法 / 没有合适方法
     */
    NoAcceptable((byte) 0xFF),
    ;
    final byte byteCode;

    public static METHOD valueOfCode(byte byteCode) {
      return Arrays.stream(values()).filter(v -> v.byteCode == byteCode).findFirst().orElse(null);
    }
  }

  @Getter
  @Accessors(fluent = true)
  @AllArgsConstructor
  public enum REP {
    Succeeded((byte) 0x00),
    GeneralSocksServerFailure((byte) 0x01),
    ConnectionNotAllowedByRuleset((byte) 0x02),
    NetworkUnreachable((byte) 0x03),
    HostUnreachable((byte) 0x04),
    ConnectionRefused((byte) 0x05),
    TtlExpired((byte) 0x06),
    CommandNotSupported((byte) 0x07),
    AddressTypeNotSupported((byte) 0x08),
    /**
     * 0x09 - 0xff
     */
    Unassigned((byte) 0x09),
    ;
    final byte byteCode;

    public static REP valueOfCode(byte byteCode) {
      return Arrays.stream(values()).filter(v -> v.byteCode == byteCode).findFirst().orElse(null);
    }
  }


  @Getter
  @Accessors(fluent = true)
  @AllArgsConstructor
  public enum ATYP {
    /**
     * ip4地址 DST.ADDR为4字节ip地址
     */
    IPv4((byte) 0x01),
    /**
     * 域名 DST.ADDR 第一个字节代表接下来有多少字节表示目标地址
     */
    Host((byte) 0x03),
    /**
     * ip6 DST.ADDR 为32字节ip地址
     */
    IPv6((byte) 0x04),

    ;
    final byte byteCode;

    public static ATYP valueOfCode(byte byteCode) {
      return Arrays.stream(values()).filter(v -> v.byteCode == byteCode).findFirst().orElse(null);
    }
  }

}
