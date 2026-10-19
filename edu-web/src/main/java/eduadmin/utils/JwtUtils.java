package eduadmin.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

public class JwtUtils {

    // HS256密钥，至少32字符
    private static final SecretKey KEY = Keys.hmacShaKeyFor(
            "aXRoZWl0YQ==aXRoZWl0YQ==aXRoZWl0YQ==".getBytes()
    );

    // 过期时间：2小时（毫秒）。压短是为了缩小令牌泄露后的可用窗口
    private static final long EXPIRATION = 2 * 3600 * 1000L;


    //生成JWT令牌
    public static String generateToken(Map<String, Object> dataMap) {
        return Jwts.builder()
                .claims(dataMap)                                        // 添加自定义数据
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION)) // 过期时间 = 当前时间 + EXPIRATION
                .signWith(KEY, Jwts.SIG.HS256)                         // 使用HS256算法和密钥签名
                .compact();                                             // 生成令牌
    }

    // 解析JWT令牌
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(KEY)              // 设置验证密钥
                .build()                      // 构建解析器实例
                .parseSignedClaims(token)     // 解析令牌
                .getPayload();                // 获取自定义数据部分
    }
}