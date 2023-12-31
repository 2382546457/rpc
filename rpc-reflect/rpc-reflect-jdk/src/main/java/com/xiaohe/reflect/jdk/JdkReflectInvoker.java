package com.xiaohe.reflect.jdk;

import com.xiaohe.spi.annotation.SPIClass;
import com.xiaohe.reflect.api.ReflectInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

@SPIClass
public class JdkReflectInvoker implements ReflectInvoker {

    private final Logger logger = LoggerFactory.getLogger(JdkReflectInvoker.class);

    @Override
    public Object invokeMethod(Object serviceBean, Class<?> serviceClass, String methodName, Class<?>[] parameterTypes, Object[] parameters) throws Throwable {
        logger.info("use jdk reflect type invoke method...");
        Method method = serviceClass.getMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(serviceBean, parameters);
    }
}