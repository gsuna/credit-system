package com.loan.service.cache;

import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedissonCacheService {

    private final RedissonClient redissonClient;

    public RedissonCacheService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public <T> void put(String key, T value, long ttlInSeconds) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(value, ttlInSeconds, TimeUnit.SECONDS);
    }

    public <T> T get(String key) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    public boolean delete(String key) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        return bucket.delete();
    }

    public <T> boolean compareAndSet(String key, T expectedValue, T newValue) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.compareAndSet(expectedValue, newValue);
    }

    public RLock getLock(String lockKey) {
        return redissonClient.getLock(lockKey);
    }

    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
        RLock lock = redissonClient.getLock(lockKey);
        return lock.tryLock(waitTime, leaseTime, unit);
    }

    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isLocked() && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
