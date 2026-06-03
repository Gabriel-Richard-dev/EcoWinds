export interface EspDevice {
  id: number;
  macAddress: string;
  ipAddress: string;
  connectionStatus: boolean;
  infraredFrequency: string;
  roomId: number | null;
  lastHeartbeatAt: string | null;
}
