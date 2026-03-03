package com.rekomenda.api.domain.room;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class RoomRepository {

    private static final String KEY_PREFIX = "room:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;
    private final JsonMapper jsonMapper;

    public RoomRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.jsonMapper = JsonMapper.builder().build();
    }

    public void save(Room room) {
        var key = KEY_PREFIX + room.getId();
        redisTemplate.opsForValue().set(key, room, TTL);
    }

    public Optional<Room> findById(String roomId) {
        var value = redisTemplate.opsForValue().get(KEY_PREFIX + roomId);
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof Room room) {
            return Optional.of(room);
        }
        if (value instanceof String json) {
            // valor já está em JSON; tentar desserializar para Room
            return Optional.ofNullable(jsonMapper.readValue(json, Room.class));
        }
        // Fallback para mapas ou outros tipos suportados pelo JsonMapper
        var json = jsonMapper.writeValueAsString(value);
        return Optional.ofNullable(jsonMapper.readValue(json, Room.class));
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
