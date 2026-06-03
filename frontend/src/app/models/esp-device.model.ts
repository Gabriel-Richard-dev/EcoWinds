export interface EspDevice {
  id: number;
  macAddress: string;
  ipAddress: string;
  connectionStatus: boolean;
  infraredFrequency: string;
  roomId: number | null;
  roomIdentification: string | null;
  lastHeartbeatAt: string | null;
  airOn: boolean;
}
