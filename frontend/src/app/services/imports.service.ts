import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { PageResponse } from '../core/models/page.model';
import { ScheduleImport } from '../models/schedule-import.model';

@Injectable({ providedIn: 'root' })
export class ImportsService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/api/imports`;

  list(page = 0, size = 20): Observable<PageResponse<ScheduleImport>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<ScheduleImport>>(this.url, { params });
  }

  upload(file: File): Observable<ScheduleImport> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ScheduleImport>(`${this.url}/upload`, form);
  }

  triggerFolderScan(): Observable<{ processed: number }> {
    return this.http.post<{ processed: number }>(`${this.url}/trigger`, {});
  }
}
