export type ImportSource = 'FOLDER_POLL' | 'UPLOAD' | 'MANUAL_TRIGGER';

export type ImportStatus =
  | 'PENDING'
  | 'SUCCESS'
  | 'SKIPPED_DUPLICATE'
  | 'PARSE_ERROR'
  | 'VALIDATION_ERROR'
  | 'PERSISTENCE_ERROR';

export interface ScheduleImport {
  id: number;
  filename: string;
  fileHash: string;
  source: ImportSource;
  status: ImportStatus;
  parserUsed: string | null;
  roomsAffected: number;
  roomsCreated: number;
  schedulesCreated: number;
  schedulesDeleted: number;
  errorMessage: string | null;
  startedAt: string;
  finishedAt: string | null;
}
