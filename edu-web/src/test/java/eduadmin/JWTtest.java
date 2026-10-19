package eduadmin;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JWTtest {


    // HS256密钥，至少32字符
    //Keys.hmacShaKeyFor:Keys是 jjwt 新版提供的工具类。hmacShaKeyFor()：把字节数组转换成符合 HMAC 算法标准的密钥对象 SecretKey。
    private final SecretKey key = Keys.hmacShaKeyFor("aXRoZWl0YQ==aXRoZWl0YQ==aXRoZWl0YQ==".getBytes());
    //getBytes()把密钥字符串转为字节数组，作为原始密钥材料传给 hmacShaKeyFor。

    @Test
    //生成令牌
    public void testGenerateJwt() {

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("id", 1);
        dataMap.put("username", "admin");

        String jwt = Jwts.builder()//构建JWT令牌
                .claims(dataMap)   // 替换旧版 addClaims()，添加自定义数据
                .expiration(new Date(System.currentTimeMillis() + 3600 * 1000)) //过期时间
                .signWith(key, Jwts.SIG.HS256) //新版签名写法，添加签名算法(HS256)和自定义密钥
                .compact();//生成

            System.out.println(jwt);
        }

    @Test
    public void testParseJWT(){
        // 这里填你运行testGenerateJwt打印出来的token字符串
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJpZCI6MSwidXNlcm5hbWUiOiJhZG1pbiIsImV4cCI6MTc4OTA1NDU4NX0.4QfJAvCsBsjFNqvt6CqD3K70s2IWhg23ipbcZwdGYQU";

        Claims claims = Jwts.parser()
                .verifyWith(key)       //旧setSigningKey → 新版verifyWith，传入SecretKey对象
                .build()               //新版必须加.build()，构建解析器实例
                .parseSignedClaims(token) //parseClaimsJws → parseSignedClaims  解析传入的令牌
                .getPayload();         //getBody() → getPayload()   获取创建令牌传入的自定义信息部分，返回一个Claims对象


        System.out.println(claims);//{id=1, username=admin, exp=1789054585}
        System.out.println(claims.get("id"));
        System.out.println(claims.get("username"));
        System.out.println(claims.get("exp"));//过期时间（这个数字代表：从`1970-01-01 00:00:00 UTC`到过期时刻一共多少秒）
    }



}
