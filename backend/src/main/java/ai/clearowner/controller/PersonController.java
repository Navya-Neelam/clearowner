package ai.clearowner.controller;

import ai.clearowner.dto.Directorship;
import ai.clearowner.dto.Holding;
import ai.clearowner.dto.PersonDetail;
import ai.clearowner.service.PersonService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/persons")
@Validated
public class PersonController {

    private final PersonService people;

    public PersonController(PersonService people) {
        this.people = people;
    }

    @GetMapping("/{personId}")
    public PersonDetail detail(@PathVariable String personId) {
        return people.detail(personId);
    }

    /** Every company this person controls, directly or through intermediaries. */
    @GetMapping("/{personId}/holdings")
    public List<Holding> holdings(
            @PathVariable String personId,
            @RequestParam(defaultValue = "6") @Min(1) @Max(8) int maxDepth,
            @RequestParam(defaultValue = "1.0") @DecimalMin("0.0") @DecimalMax("100.0") double threshold) {
        return people.holdings(personId, maxDepth, threshold);
    }

    @GetMapping("/{personId}/directorships")
    public List<Directorship> directorships(@PathVariable String personId) {
        return people.directorships(personId);
    }
}
