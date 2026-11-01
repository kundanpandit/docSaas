package com.docsaas.api.service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * In-memory sliding-window limiter. Good enough for a single instance;
 * move to Redis or the database when running more than one API instance.
 */
@Service
public class RateLimiter {

    private static final long WINDOW_MS = 60 * 60 * 1000L;

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    /** Records a hit and returns true if the key is still within its hourly limit. */
    public boolean tryAcquire(String key, int limitPerHour) {
        long now = System.currentTimeMillis();
        Deque<Long> window = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && window.peekFirst() < now - WINDOW_MS) {
                window.pollFirst();
            }
            if (window.size() >= limitPerHour) {
                return false;
            }
            window.addLast(now);
            return true;
        }
    }

    /** Drops keys with no recent hits so the map does not grow forever. */
    public void prune() {
        long cutoff = System.currentTimeMillis() - WINDOW_MS;
        hits.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                Long last = e.getValue().peekLast();
                return last == null || last < cutoff;
            }
        });
    }
}
