import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CreateHolidayRequest, Holiday } from '../models/holiday.model';

export interface TodayHoliday {
  date: string;
  isHoliday: boolean;
  holidays: Holiday[];
}

@Injectable({ providedIn: 'root' })
export class HolidaysService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/api/holidays`;

  list(year?: number): Observable<Holiday[]> {
    let params = new HttpParams();
    if (year != null) params = params.set('year', year);
    return this.http.get<Holiday[]>(this.url, { params });
  }

  today(): Observable<TodayHoliday> {
    return this.http.get<TodayHoliday>(`${this.url}/today`);
  }

  create(req: CreateHolidayRequest): Observable<Holiday> {
    return this.http.post<Holiday>(this.url, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  sync(year?: number): Observable<{ year: number; upserts: number }> {
    let params = new HttpParams();
    if (year != null) params = params.set('year', year);
    return this.http.post<{ year: number; upserts: number }>(`${this.url}/sync`, null, { params });
  }
}
