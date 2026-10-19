package eduadmin.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)//只能标注在方法上，Controller 接口方法上加这个注解才生效。
@Retention(RetentionPolicy.RUNTIME)//运行时保留注解，JVM 运行阶段可以通过反射获取该注解，AOP 切面才能扫描识别。
public @interface Log {//切点表达式
}
