import { request } from './apiClient';

export interface RuntimeSettingsStatus {
  hasSettings: boolean;
  hasTelegramToken: boolean;
  hasTelegramChatId: boolean;
  hasGoogleDriveServiceAccount: boolean;
  hasGoogleDriveFolderId: boolean;
  hasBaseUrl: boolean;
}

export interface RuntimeSettingsPayload {
  telegramBotToken?: string;
  telegramChatId?: string;
  googleDriveServiceAccountJson?: string;
  googleDriveFolderId?: string;
  baseUrl?: string;
}

export function getRuntimeSettingsStatus() {
  return request<RuntimeSettingsStatus>('/api/v1/runtime-settings');
}

export function updateRuntimeSettings(payload: RuntimeSettingsPayload) {
  return request<void>('/api/v1/runtime-settings', {
    method: 'PUT',
    body: payload,
  });
}

export function clearRuntimeSettings() {
  return request<void>('/api/v1/runtime-settings', {
    method: 'DELETE',
  });
}

export function updateUserTelegramChatId(chatId: string) {
  return request<void>('/api/v1/auth/me/telegram-chat-id', {
    method: 'PUT',
    body: { chatId },
  });
}
