package ua.kidlearn.lessons;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Minimal admin content entry (create lesson + version + publish) — a STOPGAP
 * until the AI generation pipeline and moderation UI exist. Publishing here
 * is a plain status flip with no review workflow.
 */
@Service
public class LessonService {

	private static final Set<String> VALID_GENERATED_BY = Set.of("ai", "human", "ai_edited");

	private final LessonRepository lessonRepository;
	private final LessonVersionRepository lessonVersionRepository;
	private final ModuleRepository moduleRepository;
	private final ObjectMapper objectMapper;

	public LessonService(LessonRepository lessonRepository, LessonVersionRepository lessonVersionRepository,
			ModuleRepository moduleRepository, ObjectMapper objectMapper) {
		this.lessonRepository = lessonRepository;
		this.lessonVersionRepository = lessonVersionRepository;
		this.moduleRepository = moduleRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public Lesson createLesson(String moduleCode, String title) {
		Module module = moduleRepository.findByCode(moduleCode)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Unknown moduleCode: " + moduleCode));
		return lessonRepository.save(new Lesson(module.getId(), null, title));
	}

	@Transactional
	public LessonVersion createVersion(UUID lessonId, JsonNode scenario, String generatedBy) {
		lessonRepository.findById(lessonId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
		if (scenario == null || !scenario.isObject()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "scenario must be a JSON object");
		}
		if (!VALID_GENERATED_BY.contains(generatedBy)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid generatedBy: " + generatedBy);
		}
		int nextVersionNo = lessonVersionRepository.findFirstByLessonIdOrderByVersionNoDesc(lessonId)
				.map(v -> v.getVersionNo() + 1)
				.orElse(1);
		return lessonVersionRepository.save(
				new LessonVersion(lessonId, nextVersionNo, objectMapper.writeValueAsString(scenario), generatedBy));
	}

	@Transactional
	public LessonVersion publish(UUID lessonVersionId) {
		LessonVersion version = lessonVersionRepository.findById(lessonVersionId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson version not found"));
		version.publish();
		Lesson lesson = lessonRepository.findById(version.getLessonId())
				.orElseThrow(() -> new IllegalStateException("Lesson missing for version " + lessonVersionId));
		lesson.setCurrentVersionId(version.getId());
		return version;
	}

	@Transactional(readOnly = true)
	public List<CatalogEntry> listPublishedCatalog() {
		List<Lesson> published = lessonRepository.findByCurrentVersionIdIsNotNull();
		Map<UUID, Module> modulesById = moduleRepository.findAllById(published.stream().map(Lesson::getModuleId).toList())
				.stream()
				.collect(Collectors.toMap(Module::getId, m -> m));
		return published.stream()
				.map(lesson -> new CatalogEntry(lesson.getId(), lesson.getTitle(),
						modulesById.get(lesson.getModuleId()).getCode(), lesson.getCurrentVersionId()))
				.toList();
	}

}
