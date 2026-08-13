package ai.clearowner.service;

import ai.clearowner.dto.SearchResult;
import ai.clearowner.repository.CompanyRepository;
import ai.clearowner.repository.PersonRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {

    private final CompanyRepository companies;
    private final PersonRepository people;

    public SearchService(CompanyRepository companies, PersonRepository people) {
        this.companies = companies;
        this.people = people;
    }

    /** Companies lead the results because they are what an analyst starts from. */
    public List<SearchResult> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<SearchResult> results = new ArrayList<>(companies.search(query.trim(), limit));
        if (results.size() < limit) {
            results.addAll(people.search(query.trim(), limit - results.size()));
        }
        return results;
    }
}
