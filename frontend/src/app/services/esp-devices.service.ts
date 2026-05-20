import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { EspDevice } from '../models/esp-device.model';
import { BaseCrudService } from './base-crud.service';

@Injectable({ providedIn: 'root' })
export class EspDevicesService extends BaseCrudService<EspDevice> {
  protected override readonly resourceUrl = `${environment.apiBaseUrl}/esp-device`;

  constructor(http: HttpClient) {
    super(http);
  }

  swipe(id: number): Observable<void> {
    return this.http.post<void>(`${this.resourceUrl}/swipe/${id}`, null);
  }
}
