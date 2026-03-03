package com.rekomenda.api.domain.room;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically checks all active rooms and expires those that have exceeded their TTL.
 * Runs every minute; the actual TTL check is delegated to RoomService to keep scheduling
 * logic cleanly separated from business rules.
 */
@Component
public class RoomCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(RoomCleanupScheduler.class);

    private final RoomRepository roomRepository;
    private final RoomService roomService;

    public RoomCleanupScheduler(RoomRepository roomRepository, RoomService roomService) {
        this.roomRepository = roomRepository;
        this.roomService = roomService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void cleanExpiredRooms() {
        var roomIds = roomRepository.findAllRoomIds();
        if (roomIds.isEmpty()) return;

        log.debug("Running room cleanup, checking {} active rooms", roomIds.size());
        roomIds.forEach(roomService::expireRoom);
    }
}
