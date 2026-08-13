package ai.clearowner.dto;

import java.util.List;

/**
 * A closed ownership loop: each company owns the next and the last owns the
 * first. Real registers contain these and they obscure who ultimately benefits.
 */
public record CircularStructure(int length, List<Member> members) {

    public record Member(String companyId, String name, double percentageOfNext) {
    }
}
