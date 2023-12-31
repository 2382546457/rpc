package com.xiaohe.serialization.api;

import com.xiaohe.constants.RpcConstants;
import com.xiaohe.spi.annotation.SPI;

/**
 * @author : 小何
 * @Description : 序列化接口
 * @date : 2023-12-03 22:15
 */
@SPI(RpcConstants.SERIALIZATION_JDK)
public interface Serialization {
    /**
     * 序列化
     * @param obj
     * @return
     * @param <T>
     */
    <T> byte[] serialize(T obj);

    /**
     * 反序列化
     * @param data
     * @param clazz
     * @return
     * @param <T>
     */
    <T> T deserialize(byte[] data, Class<T> clazz);
}
