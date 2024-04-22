package io.github.orangewest.trans.spring.aop;

import io.github.orangewest.trans.util.TransUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class AutoTransAspect {

    @AfterReturning(pointcut = "@annotation(io.github.orangewest.trans.spring.annotation.AutoTrans)", returning = "methodResult")
    public Object afterReturning(JoinPoint joinPoint, Object methodResult) {
        TransUtil.trans(methodResult);
        return methodResult;
    }

}
