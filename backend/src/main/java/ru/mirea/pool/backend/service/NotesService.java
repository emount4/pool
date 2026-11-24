package ru.mirea.pool.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.pool.backend.domain.VisitNote;
import ru.mirea.pool.backend.repository.VisitNoteRepository;
import ru.mirea.pool.backend.repository.VisitRepository;
import ru.mirea.pool.backend.repository.WebUserRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class NotesService {
    private final VisitNoteRepository notes;
    private final VisitRepository visits;
    private final AuditService audit;
    private final WebUserRepository users;

    public NotesService(VisitNoteRepository notes, VisitRepository visits, AuditService audit, WebUserRepository users) {
        this.notes = notes;
        this.visits = visits;
        this.audit = audit;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> list(UUID visitId) {
        return notes.findByVisitIdOrderByPinnedDescCreatedAtDescIdAsc(visitId(visitId)).stream()
                .map(NotesService::toResponse).toList();
    }

    @Transactional
    public NoteResponse create(UUID visitId, UUID author, String content, boolean pinned) {
        long parent = visitId(visitId);
        VisitNote note = notes.saveAndFlush(new VisitNote(parent, users.getReferenceById(author), content.strip(), pinned));
        audit.record("NOTE_CREATED", "VISIT", visitId, "Добавлена заметка");
        return toResponse(note);
    }

    @Transactional
    public NoteResponse update(UUID visitId, UUID id, String content, boolean pinned, long version) {
        long parent = visitId(visitId);
        if (!notes.existsByIdAndVisitId(id, parent)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Заметка не найдена.");
        }
        if (notes.updateNote(id, parent, version, content.strip(), pinned) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Заметка уже изменена. Обновите её.");
        }
        audit.record("NOTE_UPDATED", "VISIT", visitId, "Изменена заметка");
        return find(parent, id);
    }

    @Transactional
    public void delete(UUID visitId, UUID id) {
        if (notes.deleteNote(id, visitId(visitId)) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Заметка не найдена.");
        }
        audit.record("NOTE_DELETED", "VISIT", visitId, "Удалена заметка");
    }

    private NoteResponse find(long visitId, UUID id) {
        return notes.findByIdAndVisitId(id, visitId).map(NotesService::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Заметка не найдена."));
    }

    private static NoteResponse toResponse(VisitNote note) {
        return new NoteResponse(note.getId(), note.getAuthor().getId(), note.getAuthor().getDisplayName(),
                note.getContent(), note.isPinned(), note.getCreatedAt(), note.getUpdatedAt(), note.getVersion());
    }

    private long visitId(UUID id) {
        return visits.findByPublicId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Посещение не найдено.")).getId();
    }

    public record NoteResponse(UUID id, UUID authorId, String authorName, String content, boolean pinned,
                               Instant createdAt, Instant updatedAt, long version) {
    }
}
