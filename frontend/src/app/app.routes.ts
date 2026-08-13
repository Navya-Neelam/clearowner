import { Routes } from '@angular/router';

/** Screens are lazily loaded so the initial bundle stays small. */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: 'dashboard',
    title: 'Dashboard · ClearOwner',
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
  },
  {
    path: 'explore',
    title: 'Explore · ClearOwner',
    loadComponent: () =>
      import('./features/explore/explore.component').then((m) => m.ExploreComponent),
  },
  {
    path: 'insights',
    title: 'Insights · ClearOwner',
    loadComponent: () =>
      import('./features/insights/insights.component').then((m) => m.InsightsComponent),
  },
  {
    path: 'companies/:id',
    title: 'Company · ClearOwner',
    loadComponent: () =>
      import('./features/company/company.component').then((m) => m.CompanyComponent),
  },
  {
    path: 'persons/:id',
    title: 'Person · ClearOwner',
    loadComponent: () =>
      import('./features/person/person.component').then((m) => m.PersonComponent),
  },
  { path: '**', redirectTo: 'dashboard' },
];
