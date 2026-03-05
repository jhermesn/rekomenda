package com.rekomenda.api.domain.room;

import com.rekomenda.api.domain.rating.RatingRepository;
import com.rekomenda.api.domain.recommendation.dto.MovieResponse;
import com.rekomenda.api.domain.room.dto.RoomEvent;
import com.rekomenda.api.domain.room.dto.RoomResponse;
import com.rekomenda.api.domain.user.UserRepository;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RatingRepository ratingRepository;
    private final GeminiService geminiService;
    private final TmdbClient tmdbClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final String frontendUrl;
    private final long roomTtlMinutes;

    public RoomService(
            RoomRepository roomRepository,
            RatingRepository ratingRepository,
            GeminiService geminiService,
            TmdbClient tmdbClient,
            SimpMessagingTemplate messagingTemplate,
            UserRepository userRepository,
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${app.room.ttl-minutes}") long roomTtlMinutes) {
        this.roomRepository = roomRepository;
        this.ratingRepository = ratingRepository;
        this.geminiService = geminiService;
        this.tmdbClient = tmdbClient;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
        this.frontendUrl = frontendUrl;
        this.roomTtlMinutes = roomTtlMinutes;
    }

    public RoomResponse createRoom(UUID hostId) {
        var user = userRepository.findById(hostId)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado", HttpStatus.NOT_FOUND));
        var room = Room.create(hostId);
        room.getParticipants().add(RoomParticipant.join(hostId, null, user.getNome(), user.getUsername()));
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

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado", HttpStatus.NOT_FOUND));

        var wasKicked = room.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(userId) && p.isExpulso());
        if (wasKicked) {
            throw new BusinessException("Você foi removido desta sala", HttpStatus.FORBIDDEN);
        }

        var alreadyIn = room.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(userId) && !p.isExpulso());
        if (!alreadyIn) {
            room.getParticipants().add(RoomParticipant.join(userId, sessionId, user.getNome(), user.getUsername()));
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
    }

    public void generateRecommendations(String roomId, UUID hostId) {
        var room = findRoom(roomId);
        assertIsHost(room, hostId);
        if (!room.allParticipantsReady()) {
            throw new BusinessException("Todos os participantes devem estar prontos", HttpStatus.BAD_REQUEST);
        }
        generateCollectiveRecommendations(room);
    }

    public void chooseFilm(String roomId, Long movieId) {
        var room = findRoom(roomId);
        room.setStatus(RoomStatus.FINALIZADA);

        room.getFilmesRecomendados().stream()
                .filter(m -> m.id() == movieId)
                .findFirst()
                .ifPresent(room::setFilmeEscolhido);

        roomRepository.save(room);
        broadcast(roomId, RoomEvent.of(RoomEvent.EventType.FILM_CHOSEN, movieId));
    }

    public void resetRoom(String roomId, UUID hostId) {
        var room = findRoom(roomId);
        assertIsHost(room, hostId);

        room.setFilmeEscolhido(null);
        room.setFilmesRecomendados(new java.util.ArrayList<>());
        room.setStatus(RoomStatus.AGUARDANDO);

        room.getParticipants().stream()
                .filter(p -> !p.isExpulso())
                .forEach(p -> {
                    p.setStatus(ParticipantStatus.PENDENTE);
                    p.setDescricaoDesejo(null);
                });

        roomRepository.save(room);
        broadcast(roomId, RoomEvent.of(RoomEvent.EventType.ROOM_RESET, null));
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
            log.info("Iniciando geração de recomendações para a sala {}", room.getId());
            var combinedPrompt = buildCollectivePrompt(room);
            log.info("Prompt montado para Gemini: {}", combinedPrompt);
            var movies = CompletableFuture.supplyAsync(() -> {
                var keywords = geminiService.extractKeywords(combinedPrompt);
                log.info("Keywords extraídas pelo Gemini: {}", keywords);

                var genreMap = tmdbClient.fetchGenreMap();
                log.info("Mapa de gêneros TMDB: {} entradas", genreMap.size());

                var matchedGenreIds = keywords.stream()
                        .map(String::toLowerCase)
                        .map(k -> genreMap.getOrDefault(k, null))
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList();

                var unmatchedKeywords = keywords.stream()
                        .filter(k -> !genreMap.containsKey(k.toLowerCase()))
                        .toList();

                log.info("Gêneros resolvidos: {}, keywords sem match: {}", matchedGenreIds, unmatchedKeywords);

                int movieLimit = (int) (room.getParticipants().stream().filter(p -> !p.isExpulso()).count() * 2);
                int perSource = Math.max(movieLimit, 6);

                var allMovies = new java.util.ArrayList<com.rekomenda.api.infrastructure.tmdb.dto.TmdbMovie>();

                if (!matchedGenreIds.isEmpty()) {
                    allMovies.addAll(tmdbClient.discoverByGenres(matchedGenreIds, perSource));
                }

                for (var keyword : unmatchedKeywords) {
                    allMovies.addAll(tmdbClient.searchByKeywords(keyword, perSource));
                }
                return allMovies.stream()
                        .filter(m -> m.overview() != null && !m.overview().isBlank())
                        .collect(java.util.stream.Collectors.toMap(
                                com.rekomenda.api.infrastructure.tmdb.dto.TmdbMovie::id,
                                m -> m,
                                (a, b) -> a))
                        .values().stream()
                        .sorted(java.util.Comparator.comparingDouble(
                                com.rekomenda.api.infrastructure.tmdb.dto.TmdbMovie::voteAverage).reversed())
                        .limit(movieLimit)
                        .map(MovieResponse::from)
                        .toList();
            }).get(30, TimeUnit.SECONDS);

            room.setFilmesRecomendados(List.copyOf(movies));
            room.setStatus(RoomStatus.AGUARDANDO);
            roomRepository.save(room);

            log.info("Recomendações geradas com sucesso. {} filmes encontrados.", movies.size());
            broadcast(room.getId(), RoomEvent.of(RoomEvent.EventType.RECOMMENDATIONS_READY, movies));
        } catch (Exception ex) {
            log.error("Erro ao gerar recomendações coletivas para a sala {}", room.getId(), ex);
            room.setFilmesRecomendados(previousMovies);
            room.setStatus(previousStatus);
            roomRepository.save(room);

            broadcast(room.getId(), RoomEvent.of(
                    RoomEvent.EventType.RECOMMENDATIONS_FAILED,
                    "Não foi possível gerar recomendações coletivas. Tente novamente em alguns instantes."));
            throw new RuntimeException(ex);
        }
    }

    /**
     * Concatenates each active participant's rating history summary with their
     * anonymous prompt
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
