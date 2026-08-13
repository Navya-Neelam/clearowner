package ai.clearowner.controller;

import ai.clearowner.dto.SearchResult;
import ai.clearowner.service.SearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@Validated
public class SearchController {

    private final SearchService search;

    public SearchController(SearchService search) {
        this.search = search;
    }

    @GetMapping
    public List<SearchResult> search(@RequestParam String q,
                                     @RequestParam(defaultValue = "15") @Min(1) @Max(50) int limit) {
        return search.search(q, limit);
    }
}
