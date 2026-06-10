import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AcSchedule } from '../models/ac-schedule.model';

@Injectable({ providedIn: 'root' })
export class AcSchedulesService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/ac-schedule`;

  findAll(): Observable<AcSchedule[]> {
    return this.http.get<AcSchedule[]>(this.url);
  }

  create(dto: Partial<AcSchedule>): Observable<AcSchedule> {
    return this.http.post<AcSchedule>(this.url, dto);
  }

  update(id: number, dto: Partial<AcSchedule>): Observable<AcSchedule> {
    return this.http.put<AcSchedule>(`${this.url}/${id}`, dto);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
