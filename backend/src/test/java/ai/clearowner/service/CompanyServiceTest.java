package ai.clearowner.service;

import ai.clearowner.dto.CompanyDetail;
import ai.clearowner.dto.RiskSignals;
import ai.clearowner.exception.NotFoundException;
import ai.clearowner.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompanyServiceTest {

    private CompanyRepository repository;
    private CompanyService service;

    private static final CompanyDetail HAVEN_COMPANY = new CompanyDetail(
            "CO-0001", "Zephyr Capital Ltd", "ACTIVE", "Holding Company", "2001-04-02",
            "MT", "Malta", true, "191 Republic Street", "Valletta", 4, 2, 3);

    @BeforeEach
    void setUp() {
        repository = mock(CompanyRepository.class);
        service = new CompanyService(repository);
    }

    @Test
    void unknownCompanyIsReportedAsNotFound() {
        when(repository.findById("CO-9999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detail("CO-9999"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("CO-9999");
    }

    @Test
    void structuralObservationsDescribeEveryDetectedPattern() {
        when(repository.findById("CO-0001")).thenReturn(Optional.of(HAVEN_COMPANY));
        when(repository.ownershipRoutesThroughSecrecyHaven("CO-0001")).thenReturn(true);
        when(repository.partOfCircularStructure("CO-0001")).thenReturn(true);
        when(repository.companiesAtSameAddress("CO-0001")).thenReturn(10);
        when(repository.longestOwnershipChain("CO-0001")).thenReturn(5);
        when(repository.beneficialOwners(anyString(), anyInt(), anyDouble(), anyInt()))
                .thenReturn(List.of());

        RiskSignals signals = service.riskSignals("CO-0001");

        assertThat(signals.registeredInSecrecyHaven()).isTrue();
        assertThat(signals.partOfCircularStructure()).isTrue();
        assertThat(signals.companiesAtSameAddress()).isEqualTo(10);
        assertThat(signals.notes())
                .anyMatch(n -> n.contains("Malta"))
                .anyMatch(n -> n.contains("circular"))
                .anyMatch(n -> n.contains("10 companies share"))
                .anyMatch(n -> n.contains("layered 5 levels"))
                .anyMatch(n -> n.contains("25% threshold"));
    }

    @Test
    void aPlainCompanyProducesNoAlarmingLanguage() {
        CompanyDetail plain = new CompanyDetail(
                "CO-0002", "Dunmore Trading Ltd", "ACTIVE", "Private Limited Company",
                "2010-01-01", "GB", "United Kingdom", false, "12 Fenchurch Street", "London", 1, 0, 2);

        when(repository.findById("CO-0002")).thenReturn(Optional.of(plain));
        when(repository.ownershipRoutesThroughSecrecyHaven("CO-0002")).thenReturn(false);
        when(repository.partOfCircularStructure("CO-0002")).thenReturn(false);
        when(repository.companiesAtSameAddress("CO-0002")).thenReturn(1);
        when(repository.longestOwnershipChain("CO-0002")).thenReturn(1);
        when(repository.beneficialOwners(anyString(), anyInt(), anyDouble(), anyInt()))
                .thenReturn(List.of(new ai.clearowner.dto.BeneficialOwner(
                        "PER-0001", "Amara Bellamy", false, 88.0, 1, 1)));

        RiskSignals signals = service.riskSignals("CO-0002");

        assertThat(signals.notes()).containsExactly("No structural observations for this company.");
    }
}
