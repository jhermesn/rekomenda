package com.rekomenda.api.domain.room;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class RoomRepository {

    private static final String KEY_PREFIX = "room:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RoomRepository(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void save(Room room) {
        var key = KEY_PREFIX + room.getId();
        redisTemplate.opsForValue().set(key, room, TTL);
    }

    public Optional<Room> findById(String roomId) {
        var value = redisTemplate.opsForValue().get(KEY_PREFIX + roomId);
        if (value == null) return Optional.empty();
        return Optional.of(objectMapper.convertValue(value, Room.class));
    }

    public void delete(String roomId) {
        redisTemplate.delete(KEY_PREFIX + roomId);
    }

    /**
     * Returns all active Room keys for the cleanup scheduler to inspect.
     */
    public List<String> findAllRoomIds() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null) return List.of();
        return keys.stream()
                .map(k -> k.replace(KEY_PREFIX, ""))
                .toList();
    }
}
