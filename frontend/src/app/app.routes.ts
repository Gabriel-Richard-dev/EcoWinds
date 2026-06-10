import { Routes } from '@angular/router';
import { MainLayoutComponent } from './layouts/main-layout/main-layout.component';
import { authGuard, guestGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Login | EcoWinds',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./pages/login/login.page').then((component) => component.LoginPageComponent),
  },
  {
    path: 'register',
    title: 'Registrar-se | EcoWinds',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./pages/register/register.page').then((component) => component.RegisterPageComponent),
  },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        title: 'Dashboard | EcoWinds',
        loadComponent: () =>
          import('./pages/dashboard/dashboard.page').then(
            (component) => component.DashboardPageComponent,
          ),
      },
      {
        path: 'rooms',
        title: 'Salas de aula | EcoWinds',
        loadComponent: () =>
          import('./pages/rooms/rooms.page').then((component) => component.RoomsPageComponent),
      },
      {
        path: 'devices',
        title: 'Microcontroladores | EcoWinds',
        loadComponent: () =>
          import('./pages/devices/devices.page').then((component) => component.DevicesPageComponent),
      },
      {
        path: 'schedules',
        title: 'Agendamentos | EcoWinds',
        loadComponent: () =>
          import('./pages/schedules/schedules.page').then(
            (component) => component.SchedulesPageComponent,
          ),
      },
      {
        path: 'audit-logs',
        title: 'Logs do sistema | EcoWinds',
        loadComponent: () =>
          import('./pages/audit-logs/audit-logs.page').then(
            (component) => component.AuditLogsPageComponent,
          ),
      },
      {
        path: 'imports',
        title: 'Importações SIGEHO | EcoWinds',
        loadComponent: () =>
          import('./pages/imports/imports.page').then(
            (component) => component.ImportsPageComponent,
          ),
      },
      {
        path: 'holidays',
        title: 'Feriados | EcoWinds',
        loadComponent: () =>
          import('./pages/holidays/holidays.page').then(
            (component) => component.HolidaysPageComponent,
          ),
      },
      {
        path: 'ac-schedules',
        title: 'Agendamentos AC | EcoWinds',
        loadComponent: () =>
          import('./pages/ac-schedules/ac-schedules.page').then(
            (component) => component.AcSchedulesPageComponent,
          ),
      },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
