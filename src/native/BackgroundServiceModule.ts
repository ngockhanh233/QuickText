import {NativeModules, Platform} from 'react-native';

const {QuickTextService} = NativeModules;

export function startBackgroundService(): void {
  if (Platform.OS === 'android' && QuickTextService) {
    QuickTextService.startService();
  }
}

export function stopBackgroundService(): void {
  if (Platform.OS === 'android' && QuickTextService) {
    QuickTextService.stopService();
  }
}

/**
 * Opens the system "Languages & input" settings screen so the user can enable
 * the QuickText keyboard.
 */
export function openKeyboardSettings(): void {
  if (Platform.OS === 'android' && QuickTextService) {
    QuickTextService.openKeyboardSettings();
  }
}

/**
 * Shows the IME picker (keyboard chooser) so the user can switch to the
 * QuickText keyboard as the active input method.
 */
export function showKeyboardPicker(): void {
  if (Platform.OS === 'android' && QuickTextService) {
    QuickTextService.showKeyboardPicker();
  }
}

/**
 * Returns true if the QuickText keyboard has been enabled in system settings.
 */
export async function isKeyboardEnabled(): Promise<boolean> {
  if (Platform.OS !== 'android' || !QuickTextService) return false;
  try {
    return await QuickTextService.isKeyboardEnabled();
  } catch {
    return false;
  }
}
