package ua.kidlearn.lessons;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Falls under the default anyRequest().authenticated() — any signed-in adult can browse it. */
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

	private final LessonService lessonService;

	public CatalogController(LessonService lessonService) {
		this.lessonService = lessonService;
	}

	@GetMapping("/lessons")
	public List<CatalogEntry> lessons() {
		return lessonService.listPublishedCatalog();
	}

}
