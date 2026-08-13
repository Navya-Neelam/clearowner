import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  AddressCluster,
  BeneficialOwner,
  CircularStructure,
  CompanyDetail,
  DashboardSummary,
  DirectOwner,
  Directorship,
  GraphView,
  Health,
  Holding,
  PersonDetail,
  RiskSignals,
  SearchResult,
  TopController,
} from './models';

/** The single place the frontend knows the API's shape. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  health(): Observable<Health> {
    return this.http.get<Health>(`${this.base}/api/health`);
  }

  summary(): Observable<DashboardSummary> {
    return this.http.get<DashboardSummary>(`${this.base}/api/dashboard/summary`);
  }

  search(query: string, limit = 15): Observable<SearchResult[]> {
    const params = new HttpParams().set('q', query).set('limit', limit);
    return this.http.get<SearchResult[]>(`${this.base}/api/search`, { params });
  }

  company(id: string): Observable<CompanyDetail> {
    return this.http.get<CompanyDetail>(`${this.base}/api/companies/${encodeURIComponent(id)}`);
  }

  directOwners(id: string): Observable<DirectOwner[]> {
    return this.http.get<DirectOwner[]>(
      `${this.base}/api/companies/${encodeURIComponent(id)}/direct-owners`,
    );
  }

  beneficialOwners(id: string, maxDepth = 6, threshold = 25): Observable<BeneficialOwner[]> {
    const params = new HttpParams().set('maxDepth', maxDepth).set('threshold', threshold);
    return this.http.get<BeneficialOwner[]>(
      `${this.base}/api/companies/${encodeURIComponent(id)}/beneficial-owners`,
      { params },
    );
  }

  companyGraph(id: string, depth = 3): Observable<GraphView> {
    const params = new HttpParams().set('depth', depth);
    return this.http.get<GraphView>(
      `${this.base}/api/companies/${encodeURIComponent(id)}/graph`,
      { params },
    );
  }

  companyDirectors(id: string): Observable<Directorship[]> {
    return this.http.get<Directorship[]>(
      `${this.base}/api/companies/${encodeURIComponent(id)}/directors`,
    );
  }

  riskSignals(id: string): Observable<RiskSignals> {
    return this.http.get<RiskSignals>(
      `${this.base}/api/companies/${encodeURIComponent(id)}/risk-signals`,
    );
  }

  person(id: string): Observable<PersonDetail> {
    return this.http.get<PersonDetail>(`${this.base}/api/persons/${encodeURIComponent(id)}`);
  }

  personHoldings(id: string, maxDepth = 6, threshold = 1): Observable<Holding[]> {
    const params = new HttpParams().set('maxDepth', maxDepth).set('threshold', threshold);
    return this.http.get<Holding[]>(
      `${this.base}/api/persons/${encodeURIComponent(id)}/holdings`,
      { params },
    );
  }

  personDirectorships(id: string): Observable<Directorship[]> {
    return this.http.get<Directorship[]>(
      `${this.base}/api/persons/${encodeURIComponent(id)}/directorships`,
    );
  }

  circularStructures(limit = 25): Observable<CircularStructure[]> {
    const params = new HttpParams().set('limit', limit);
    return this.http.get<CircularStructure[]>(
      `${this.base}/api/insights/circular-structures`,
      { params },
    );
  }

  sharedAddresses(minCompanies = 4, limit = 25): Observable<AddressCluster[]> {
    const params = new HttpParams().set('minCompanies', minCompanies).set('limit', limit);
    return this.http.get<AddressCluster[]>(
      `${this.base}/api/insights/shared-addresses`,
      { params },
    );
  }

  topControllers(minReach = 20, limit = 25): Observable<TopController[]> {
    const params = new HttpParams().set('minReach', minReach).set('limit', limit);
    return this.http.get<TopController[]>(
      `${this.base}/api/insights/top-controllers`,
      { params },
    );
  }
}
