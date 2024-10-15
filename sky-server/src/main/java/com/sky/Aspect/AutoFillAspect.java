package com.sky.Aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义切面类，对INSERT或UPDATE方法进行公共字段填充
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    @Pointcut("@annotation(com.sky.annotation.AutoFill)")
    public void pt(){
    }

    /**
     * 公共字段填充
     * @param joinPoint
     */
    @Before("pt()")
    public void autoFill(JoinPoint joinPoint) throws Throwable
    {
        log.info("公共字段填充");

        // 获取方法的数据库操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();     // 方法签名对象
        AutoFill annotation = signature.getMethod().getAnnotation(AutoFill.class);  // 方法注解对象
        OperationType operationType = annotation.value();                                   // 数据库操作类型

        // 获取方法的参数实体
        Object[] args = joinPoint.getArgs();
        Object entity = args[0];

        // 通过反射来进行公共字段的赋值
        if(operationType==OperationType.INSERT)
        {
            Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
            Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
            Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
            Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

            setCreateTime.invoke(entity,LocalDateTime.now());
            setUpdateTime.invoke(entity,LocalDateTime.now());
            setCreateUser.invoke(entity,BaseContext.getCurrentId());
            setUpdateUser.invoke(entity,BaseContext.getCurrentId());

            return;
        }


        Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
        Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

        setUpdateTime.invoke(entity,LocalDateTime.now());
        setUpdateUser.invoke(entity,BaseContext.getCurrentId());

    }
}
