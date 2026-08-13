package ai.clearowner.service;

import ai.clearowner.dto.Directorship;
import ai.clearowner.dto.Holding;
import ai.clearowner.dto.PersonDetail;
import ai.clearowner.exception.NotFoundException;
import ai.clearowner.repository.PersonRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PersonService {

    private static final int MAX_ROWS = 200;

    private final PersonRepository people;

    public PersonService(PersonRepository people) {
        this.people = people;
    }

    public PersonDetail detail(String personId) {
        return people.findById(personId)
                .orElseThrow(() -> new NotFoundException("Person", personId));
    }

    public List<Holding> holdings(String personId, int maxDepth, double threshold) {
        detail(personId);
        return people.holdings(personId, maxDepth, threshold, MAX_ROWS);
    }

    public List<Directorship> directorships(String personId) {
        detail(personId);
        return people.directorships(personId, MAX_ROWS);
    }
}
