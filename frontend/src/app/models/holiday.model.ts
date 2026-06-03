export type HolidayScope = 'NATIONAL' | 'STATE' | 'MUNICIPAL';

export interface Holiday {
  id: number;
  date: string;
  name: string;
  scope: HolidayScope;
  source: string;
  type: string | null;
}

export interface CreateHolidayRequest {
  date: string;
  name: string;
  scope: HolidayScope;
  type?: string | null;
}
