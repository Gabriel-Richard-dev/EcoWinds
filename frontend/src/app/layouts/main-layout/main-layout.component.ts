import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';

interface NavItem {
  route: string;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.scss',
})
export class MainLayoutComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly currentUrl = signal(this.router.url);
  protected readonly navItems: readonly NavItem[] = [
    { route: '/dashboard', label: 'Dashboard', icon: 'pi pi-th-large' },
    { route: '/rooms', label: 'Salas de aula', icon: 'pi pi-building' },
    { route: '/devices', label: 'Microcontroladores', icon: 'pi pi-microchip' },
    { route: '/schedules', label: 'Agendamentos', icon: 'pi pi-calendar-clock' },
    { route: '/imports', label: 'Importações SIGEHO', icon: 'pi pi-cloud-upload' },
    { route: '/holidays', label: 'Feriados', icon: 'pi pi-flag' },
    { route: '/audit-logs', label: 'Logs do sistema', icon: 'pi pi-list-check' },
  ];

  constructor() {
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe((event) => this.currentUrl.set(event.urlAfterRedirects));
  }

  protected activeItem(): NavItem {
    return this.navItems.find((item) => this.currentUrl().startsWith(item.route)) ?? this.navItems[0];
  }

  protected userName(): string {
    return this.authService.userDisplayName();
  }

  protected logout(): void {
    this.authService.logout();
  }
}
