export type DeviceType = 'SMARTPHONE' | 'TABLET' | 'LAPTOP' | 'GAME_CONSOLE' | 'OTHER';

export const DEVICE_TYPE_LABELS: Record<DeviceType, string> = {
  SMARTPHONE: 'Smartphone',
  TABLET: 'Tablet',
  LAPTOP: 'Laptop',
  GAME_CONSOLE: 'Game console',
  OTHER: 'Other'
};

export const DEVICE_TYPE_OPTIONS = Object.entries(DEVICE_TYPE_LABELS)
  .map(([value, label]) => ({ value: value as DeviceType, label }));