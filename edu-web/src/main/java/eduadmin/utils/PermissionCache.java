package eduadmin.utils;

import eduadmin.Mapper.EmpRoleMapper;
import eduadmin.Mapper.RolePermissionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/**
  权限数据的缓存层。

  决策引擎每个请求都要查两样东西：这个员工有哪些角色、这些角色有哪些权限点。
  但这两样只在授权接口里才会变，平时纯读。所以放进 Redis。

  为什么按"角色"缓存权限点，而不是把"某个人的全部权限点"打成一个 key：
  后者一次 GET 就够，看着更省。但改某个角色的权限时，得先找出所有持有这个角色的员工、
  把他们的缓存一个个删掉，等于要维护一张反向索引，漏一个就有人一直拿着旧权限。
  按角色缓存的话，改哪个角色删哪个 key，不会漏。

  这一层不假设 Redis 一定可用：读写失败都退化成"没缓存"，调用方照常查库。
  缓存是加速手段，不能变成鉴权的新故障点。
 */
@Slf4j
@Component
public class PermissionCache {

    private static final String EMP_ROLES = "perm:emp:roles:";
    private static final String ROLE_CODES = "perm:role:codes:";

    /* 兜底过期。正常靠授权接口主动删 key，这个只是防止哪天漏删了数据永远不刷新 */
    private static final long TTL_MINUTES = 30;

    /* TTL 抖动范围（秒）。固定 TTL 会让一批 key 在同一时刻集体过期，
       那一瞬间请求全落到数据库上，也就是缓存雪崩。上下浮动几分钟就够了 */
    private static final long TTL_JITTER_SECONDS = 300;

    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private EmpRoleMapper empRoleMapper;
    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    /**
      查员工有哪些角色。缓存没命中就查库并回填。
      返回空集合表示这个人确实没分配角色，不是"查不出来"。
     */
    public List<Integer> loadRoleIds(Integer empId) {
        String key = EMP_ROLES + empId;
        List<Integer> cached = readList(key, Integer::valueOf);//s -> Integer.valueOf(s)对每个元素调用 Integer.valueOf()
        if (cached != null) {
            return cached;
        }
        List<Integer> roleIds = empRoleMapper.selectRoleIdsByEmpId(empId);
        write(key, join(roleIds));
        return roleIds == null ? new ArrayList<>() : roleIds;
    }

    /**
      查这些角色合起来有哪些权限点。
      逐个角色看缓存，没命中的才去查库，查完按角色回填。
      角色通常只有一两个，多几次 GET 无所谓；换成"一次查全部"反而没办法按角色回填。
     */
    public List<String> loadCodes(List<Integer> roleIds) {
        List<String> codes = new ArrayList<>();
        for (Integer roleId : roleIds) {
            String key = ROLE_CODES + roleId;
            List<String> cached = readList(key, s -> s);
            if (cached != null) {
                codes.addAll(cached);
                continue;
            }
            List<String> fromDb = rolePermissionMapper.selectCodesByRoleId(roleId);
            write(key, join(fromDb));
            if (fromDb != null) {
                codes.addAll(fromDb);
            }
        }
        return codes;
    }

    /**
      员工的角色变了之后调用。
      只删这一个人的，因为员工的角色只影响他自己。
     */
    public void evictEmpRoles(Integer empId) {
        delete(EMP_ROLES + empId);
    }

    /**
      角色的权限点变了之后调用。
      这个角色的 key 一删，所有持有它的员工下次请求都会重新算，不用挨个去清。
     */
    public void evictRoleCodes(Integer roleId) {
        delete(ROLE_CODES + roleId);
    }

    /* ---------- 下面都是 Redis 的读写细节，出错一律降级 ---------- */

    /**
      读一个列表。返回 null 表示没有缓存。
      空集合会被存成空字符串，和"没缓存"区分开，不然没角色的员工每次都要查库。
     */
    private <T> List<T> readList(String key, Function<String, T> parser) {/**Function<A, B>是JDK自带的接口，表示转换规则
                                                                            含义只有一句：传入一个A，返回一个B。它只有一个方法apply*/
        try {
            String raw = redis.opsForValue().get(key);
            if (raw == null) {
                return null;
            }
            if (raw.isEmpty()) {
                return new ArrayList<>();
            }
            List<T> list = new ArrayList<>();
            for (String part : raw.split(",")) {
                list.add(parser.apply(part));//在集合中添加经过parser处理的元素
            }
            return list;
        } catch (Exception e) {
            //读失败当成没缓存，让调用方去查库。不能把异常往上抛，否则鉴权直接 500
            log.warn("读权限缓存失败, 按未命中处理, key:{}", key, e);
            return null;
        }
    }

    private void write(String key, String value) {
        try {
            redis.opsForValue().set(key, value, randomTtl());//随机时效
        } catch (Exception e) {
            //写失败不影响这次请求，结果已经算出来了；下次读不到自然会再查库
            log.warn("写权限缓存失败, 不影响本次结果, key:{}", key, e);
        }
    }

    /** 30 分钟上下浮动 5 分钟，让这批 key 不会同时过期 */
    private Duration randomTtl() {
        long offset = ThreadLocalRandom.current().nextLong(-TTL_JITTER_SECONDS, TTL_JITTER_SECONDS + 1);
        return Duration.ofMinutes(TTL_MINUTES).plusSeconds(offset);
    }

    private void delete(String key) {
        try {
            redis.delete(key);
        } catch (Exception e) {
            //删失败的话，旧数据最多活到 TTL 到期，这是 TTL 存在的意义
            log.warn("删权限缓存失败, 数据最多陈旧到一个TTL, key:{}", key, e);
        }
    }

    /**
      用逗号拼成字符串存进 Redis。
      角色ID和权限点编码里都不会出现逗号，所以这样拼是安全的，
      比上 JSON 少一层序列化，redis-cli 里看得也清楚。

      把集合处理为StringBuilder，然后逐个 append最后转成 String 返回
     */
    private String join(List<?> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object item : list) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(item);
        }
        return sb.toString();
    }
}
