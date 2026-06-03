import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { EspDevice } from '../models/esp-device.model';
import { BaseCrudService } from './base-crud.service';

export interface ApiKeyRotationResponse {
  deviceId: string;
  apiKey: string;
  warning: string;
}

@Injectable({ providedIn: 'root' })
export class EspDevicesService extends BaseCrudService<EspDevice> {
  protected override readonly resourceUrl = `${environment.apiBaseUrl}/esp-device`;

  constructor(http: HttpClient) {
    super(http);
  }

  rotateApiKey(id: number): Observable<ApiKeyRotationResponse> {
    return this.http.post<ApiKeyRotationResponse>(`${this.resourceUrl}/${id}/api-key`, {});
  }
}
