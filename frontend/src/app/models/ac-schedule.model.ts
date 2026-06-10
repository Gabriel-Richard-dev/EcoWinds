export type AcAction = 'ON' | 'OFF';

export type DayOfWeek =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

export interface AcSchedule {
  id: number;
  dayOfWeek: DayOfWeek;
  time: string;
  action: AcAction;
  enabled: boolean;
}
