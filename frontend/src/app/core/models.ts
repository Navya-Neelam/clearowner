export interface SearchResult {
  type: 'Company' | 'Person';
  id: string;
  name: string;
  subtitle: string;
}

export interface CompanyDetail {
  companyId: string;
  name: string;
  status: string;
  companyType: string;
  incorporationDate: string;
  jurisdictionCode: string;
  jurisdictionName: string;
  secrecyHaven: boolean;
  addressLine: string;
  addressCity: string;
  directOwnerCount: number;
  subsidiaryCount: number;
  directorCount: number;
}

export interface DirectOwner {
  type: 'Company' | 'Person';
  id: string;
  name: string;
  percentage: number;
  shareClass: string;
  since: string;
}

export interface BeneficialOwner {
  personId: string;
  name: string;
  pep: boolean;
  effectivePercentage: number;
  routes: number;
  shortestPathLength: number;
}

export interface GraphNode {
  id: string;
  label: string;
  type: string;
  sublabel: string;
  flagged: boolean;
  focus: boolean;
}

export interface GraphEdge {
  source: string;
  target: string;
  type: string;
  percentage: number | null;
}

export interface GraphView {
  nodes: GraphNode[];
  edges: GraphEdge[];
}

export interface PersonDetail {
  personId: string;
  name: string;
  birthYear: number;
  pep: boolean;
  nationalityCode: string;
  nationalityName: string;
  directHoldingCount: number;
  directorshipCount: number;
}

export interface Holding {
  companyId: string;
  name: string;
  jurisdictionName: string;
  secrecyHaven: boolean;
  effectivePercentage: number;
  shortestPathLength: number;
}

export interface Directorship {
  personId: string;
  personName: string;
  companyId: string;
  companyName: string;
  role: string;
  appointedOn: string;
  active: boolean;
}

export interface RiskSignals {
  companyId: string;
  registeredInSecrecyHaven: boolean;
  ownershipRoutesThroughSecrecyHaven: boolean;
  partOfCircularStructure: boolean;
  companiesAtSameAddress: number;
  longestOwnershipChain: number;
  beneficialOwnersAboveThreshold: number;
  notes: string[];
}

export interface AddressCluster {
  addressId: string;
  line: string;
  city: string;
  jurisdictionName: string;
  companyCount: number;
  companies: { companyId: string; name: string }[];
}

export interface TopController {
  personId: string;
  name: string;
  pep: boolean;
  companiesReached: number;
  maxDepth: number;
}

export interface CircularStructure {
  length: number;
  members: { companyId: string; name: string; percentageOfNext: number }[];
}

export interface DashboardSummary {
  companies: number;
  people: number;
  jurisdictions: number;
  addresses: number;
  ownershipLinks: number;
  directorships: number;
  secrecyHavenCompanies: number;
  circularStructures: number;
  topAddressClusters: AddressCluster[];
  topControllers: TopController[];
}

export interface Health {
  status: string;
  databaseReachable: boolean;
  latencyMs: number;
  detail: string;
}
