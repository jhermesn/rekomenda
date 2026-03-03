package com.rekomenda.api.domain.room;

import com.rekomenda.api.domain.rating.RatingRepository;
import com.rekomenda.api.domain.recommendation.dto.MovieResponse;
import com.rekomenda.api.domain.room.dto.RoomEvent;
import com.rekomenda.api.domain.room.dto.RoomResponse;
import com.rekomenda.api.infrastructure.ai.GeminiService;
import com.rekomenda.api.infrastructure.tmdb.TmdbClient;
import com.rekomenda.api.shared.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RatingRepository ratingRepository;
    private final GeminiService geminiService;
    private final TmdbClient tmdbClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final String frontendUrl;
    private final long roomTtlMinutes;

    public RoomService(
            RoomRepository roomRepository,
            RatingRepository ratingRepository,
            GeminiService geminiService,
            TmdbClient tmdbClient,
            SimpMessagingTemplate messagingTemplate,
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${app.room.ttl-minutes}") long roomTtlMinutes
    ) {
        this.roomRepository = roomRepository;
        this.ratingRepository = ratingRepository;
        this.geminiService = geminiService;
        this.tmdbClient = tmdbClient;
        this.messagingTemplate = messagingTemplate;
        this.frontendUrl = frontendUrl;
        this.roomTtlMinutes = roomTtlMinutes;
    }

    public RoomResponse createRoom(UUID hostId) {
        var room = Room.create(hostId);
        room.getParticipants().add(RoomParticipant.join(hostId, null));
        roomRepository.save(room);
        return RoomResponse.from(room, frontendUrl);
    }

    public RoomResponse getRoom(String roomId) {
        var room = findRoom(roomId);
        return RoomResponse.from(room, frontendUrl);
    }

    public void joinRoom(String roomId, UUID userId, String sessionId) {
        var room = findRoom(roomId);
        assertRoomActive(room);

        var wasKicked = room.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(userId) && p.isExpulso());
        if (wasKicked) {
            throw new BusinessException("Você foi removido desta sala", HttpStatus.FORBIDDEN);
        }

        var alreadyIn = room.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(userId) && !p.isExpulso());
        if (!alreadyIn) {
            room.getParticipants().add(RoomParticipant.join(userId, sessionId));
            roomRepository.save(room);
        }

        broadcast(roomId, RoomEvent.of(RoomEvent.EventType.PARTICIPANT_JOINED, userId));
    }

    public void leaveRoom(String roomId, UUID userId) {
        var room = findRoom(roomId);
        room.getParticipants().removeIf(p -> p.getUserId().equals(userId));
        roomRepository.save(room);
        broadcast(roomId, RoomEvent.of(RoomEvent.EventType.PARTICIPANT_LEFT, userId));
    }

    public void kickParticipant(String roomId, UUID hostId, UUID targetUserId) {
        var room = findRoom(roomId);
        assertIsHost(room, hostId);

        room.getParticipants().stream()
                .filter(p -> p.getUserId().equals(targetUserId))
                .findFirst()
                .ifPresent(p -> p.setExpulso(true));

        roomRepository.save(room);
        broadcast(roomId, RoomEvent.of(RoomEvent.EventType.PARTICIPANT_KICKED, targetUserId));
    }

    public void submitPrompt(String roomId, UUID userId, String descricao) {
        var room = findRoom(roomId);
        assertRoomActive(room);

        room.getParticipants().stream()
                .filter(p -> p.getUserId().equals(userId) && !p.isExpulso())
                .findFirst()
                .ifPresent(p -> {
                    p.setDescricaoDesejo(descricao);
                    p.setStatus(ParticipantStatus.PRONTO);
                });

        roomRepository.save(room);
        broadcast(roomId, RoomEvent.of(RoomEvent.EventType.PARTICIPANT_READY, userId));

        if (room.allParticipantsReady()) {
            generateCollectiveRecommendations(room);
        }
    }

    public void requestMoreRecommendations(String roomId) {
        var room = findRoom(roomId);
        generateCollectiveRecommendations(room);
    }

    public void chooseFilm(String roomId, Long movieId) {
        var room = findRoom(roomId);
        room.setStatus(RoomStatus.FINALIZADA);
        roomRepository.save(room);
        broadcast(roomId, RoomEvent.of(RoomEvent.EventType.FILM_CHOSEN, movieId));
    }

    public void closeRoom(String roomId, UUID hostId) {
        var room = findRoom(roomId);
        assertIsHost(room, hostId);
        terminateRoom(room, RoomEvent.EventType.ROOM_CLOSED);
    }

    public void handleHostDisconnect(String sessionId) {
        roomRepository.findAllRoomIds().stream()
                .map(roomRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(room -> room.getParticipants().stream()
                        .anyMatch(p -> p.getUserId().equals(room.getHostId())
                                && sessionId.equals(p.getSessionId())))
                .forEach(room -> terminateRoom(room, RoomEvent.EventType.HOST_DISCONNECTED));
    }

    /**
     * Called by the cleanup scheduler for rooms that have exceeded their TTL.
     */
    public void expireRoom(String roomId) {
        roomRepository.findById(roomId).ifPresent(room -> {
            if (room.isExpired(roomTtlMinutes)) {
                terminateRoom(room, RoomEvent.EventType.ROOM_CLOSED);
            }
        });
    }

    private void generateCollectiveRecommendations(Room room) {
        var previousStatus = room.getStatus();
        var previousMovies = List.copyOf(room.getFilmesRecomendados());

        room.setStatus(RoomStatus.GERANDO_RECOMENDACOES);
        roomRepository.save(room);

        try {
            var combinedPrompt = buildCollectivePrompt(room);
            var keywords = geminiService.extractKeywords(combinedPrompt);
            var movieLimit = room.getParticipants().stream().filter(p -> !p.isExpulso()).count() * 2;

            var movies = keywords.stream()
                    .flatMap(keyword -> tmdbClient.searchByKeywords(keyword, (int) movieLimit).stream())
                    .distinct()
                    .limit(movieLimit)
                    .map(MovieResponse::from)
                    .toList();

            room.setFilmesRecomendados(List.copyOf(movies));
            room.setStatus(RoomStatus.AGUARDANDO);
            roomRepository.save(room);

            broadcast(room.getId(), RoomEvent.of(RoomEvent.EventType.RECOMMENDATIONS_READY, movies));
        } catch (RuntimeException ex) {
            // rollback para o último estado consistente
            room.setFilmesRecomendados(previousMovies);
            room.setStatus(previousStatus);
            roomRepository.save(room);

            broadcast(room.getId(), RoomEvent.of(
                    RoomEvent.EventType.RECOMMENDATIONS_FAILED,
                    "Não foi possível gerar recomendações coletivas. Tente novamente em alguns instantes."
            ));
            throw ex;
        }
    }

    /**
     * Concatenates each active participant's rating history summary with their anonymous prompt
     * to create a rich context for the LLM.
     */
    private String buildCollectivePrompt(Room room) {
        return room.getParticipants().stream()
                .filter(p -> !p.isExpulso())
                .map(p -> {
                    var ratings = ratingRepository.findByUserIdOrderByDataAvaliacaoDesc(p.getUserId());
                    var ratingSummary = ratings.stream()
                            .limit(10)
                            .map(r -> r.getTipo().name() + " content ID " + r.getConteudoId())
                            .collect(Collectors.joining(", "));

                    return "Participant: recent ratings=[%s], current desire=[%s]"
                            .formatted(ratingSummary, p.getDescricaoDesejo());
                })
                .collect(Collectors.joining("\n"));
    }

    private void terminateRoom(Room room, RoomEvent.EventType eventType) {
        broadcast(room.getId(), RoomEvent.of(eventType, null));
        roomRepository.delete(room.getId());
    }

    private void broadcast(String roomId, RoomEvent event) {
        messagingTemplate.convertAndSend("/topic/room/" + roomId, event);
    }

    private Room findRoom(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException("Sala não encontrada", HttpStatus.NOT_FOUND));
    }

    private void assertIsHost(Room room, UUID userId) {
        if (!room.getHostId().equals(userId)) {
            throw new BusinessException("Apenas o host pode realizar esta ação", HttpStatus.FORBIDDEN);
        }
    }

    private void assertRoomActive(Room room) {
        if (RoomStatus.FINALIZADA == room.getStatus()) {
            throw new BusinessException("Esta sala já foi encerrada", HttpStatus.GONE);
        }
    }
}
