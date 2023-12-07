package com.xiaohe.consumer.common.future;

import com.xiaohe.protocol.RpcProtocol;
import com.xiaohe.protocol.request.RpcRequest;
import com.xiaohe.protocol.response.RpcResponse;
import org.apache.log4j.lf5.LF5Appender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * @author : 小何
 * @Description : 用于同步请求
 * @date : 2023-12-07 20:34
 */
public class RpcFuture extends CompletableFuture<Object> {
    private static final Logger logger = LoggerFactory.getLogger(RpcFuture.class);

    static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1L;
        /**
         * 请求已结束/完成
         */
        private final int done = 1;
        /**
         * 请求未结束/完成
         */
        private final int pending = 0;

        /**
         * 只有请求结束了返回了才能成功加锁，否则只能成为阻塞队列中的一员进行阻塞等待
         * @return
         */
        @Override
        protected boolean tryAcquire(int acquires) {
            return getState() == done;
        }

        /**
         * 释放锁。请求返回时调用，用于将
         */
        @Override
        protected boolean tryRelease(int arg) {
            // 如果是第一次接收到响应，现在的状态肯定为 pending，如果使用CAS能将 pending 改为 done，那么返回true，唤醒其他阻塞的线程
            if (getState() == pending) {
                if (compareAndSetState(pending, done)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * 任务是否结束
         */
        public boolean isDone() {
            getState();
            return getState() == done;
        }
    }

    private Sync sync;

    /**
     * 请求
     */
    private RpcProtocol<RpcRequest> requestRpcProtocol;
    /**
     * 响应
     */
    private RpcProtocol<RpcResponse> responseRpcProtocol;

    /**
     * 请求的开始时间
     */
    private long startTime;

    /**
     * 请求的持续时间，超过并不放弃，只是打印一下这个请求实在太慢了
     */
    private long responseTimeThreshold = 5000;

    public RpcFuture(RpcProtocol<RpcRequest> requestRpcProtocol) {
        this.sync = new Sync();
        this.requestRpcProtocol = requestRpcProtocol;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public boolean isDone() {
        return sync.isDone();
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        sync.acquire(-1);
        if (this.responseRpcProtocol != null) {
            return this.responseRpcProtocol.getBody().getResult();
        } else {
            return null;
        }
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean success = sync.tryAcquireNanos(-1, unit.toNanos(timeout));
        if (success) {
            if (this.responseRpcProtocol != null) {
                return this.responseRpcProtocol.getBody().getResult();
            } else {
                return null;
            }
        } else {
            throw new RuntimeException("Timeout exception. Request id : " + this.requestRpcProtocol.getHeader().getRequestId()
                    + ". Request class name : " + this.requestRpcProtocol.getBody().getClassName()
                    + ". Request method : " + this.requestRpcProtocol.getBody().getMethodName());

        }
    }

    /**
     * 当请求完成时调用的方法
     *
     * @param responseRpcProtocol
     */
    public void done(RpcProtocol<RpcResponse> responseRpcProtocol) {
        this.responseRpcProtocol = responseRpcProtocol;
        // 释放锁，将状态从pending改为done
        sync.release(1);
        // 计算此次请求时间是否超过预期
        long responseTime = System.currentTimeMillis() - startTime;
        if (responseTime > this.responseTimeThreshold) {
            logger.warn("Service response time is too slow. Request id = " + responseRpcProtocol.getHeader().getRequestId() + ", response time = " + responseTime);
        }
    }


    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }
}
