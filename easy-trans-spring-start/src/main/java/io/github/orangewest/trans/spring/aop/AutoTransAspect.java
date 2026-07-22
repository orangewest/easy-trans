package io.github.orangewest.trans.spring.aop;

import io.github.orangewest.trans.spring.uitl.TransUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class AutoTransAspect {

    @Around("@annotation(io.github.orangewest.trans.spring.annotation.AutoTrans)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        return TransUtil.trans(result);
    }

}
