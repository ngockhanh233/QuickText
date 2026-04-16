import React, {useState, useEffect, useCallback} from 'react';
import {
  Alert,
  AppState,
  StyleSheet,
  Switch,
  Text,
  TouchableOpacity,
  View,
  Platform,
  PermissionsAndroid,
  ScrollView,
} from 'react-native';
import {
  startBackgroundService,
  stopBackgroundService,
  openKeyboardSettings,
  showKeyboardPicker,
  isKeyboardEnabled,
} from '../native/BackgroundServiceModule';

export function SettingsScreen() {
  const [serviceRunning, setServiceRunning] = useState(false);
  const [keyboardEnabled, setKeyboardEnabled] = useState(false);

  const refreshKeyboardStatus = useCallback(async () => {
    const enabled = await isKeyboardEnabled();
    setKeyboardEnabled(enabled);
  }, []);

  useEffect(() => {
    refreshKeyboardStatus();
    const sub = AppState.addEventListener('change', state => {
      if (state === 'active') {
        refreshKeyboardStatus();
      }
    });
    return () => sub.remove();
  }, [refreshKeyboardStatus]);

  const handleToggleService = async (value: boolean) => {
    if (value) {
      if (Platform.OS === 'android' && Platform.Version >= 33) {
        const granted = await PermissionsAndroid.request(
          'android.permission.POST_NOTIFICATIONS' as any,
        );
        if (granted !== PermissionsAndroid.RESULTS.GRANTED) {
          Alert.alert(
            'Cần quyền thông báo',
            'Vui lòng cấp quyền thông báo để chạy dịch vụ nền.',
          );
          return;
        }
      }
      startBackgroundService();
      setServiceRunning(true);
    } else {
      stopBackgroundService();
      setServiceRunning(false);
    }
  };

  return (
    <ScrollView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Cài đặt</Text>
      </View>

      {/* KEYBOARD SECTION */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Bàn phím QuickText</Text>

        <View style={styles.card}>
          <View style={styles.statusRow}>
            <View style={styles.statusDot(keyboardEnabled)} />
            <Text style={styles.statusText}>
              {keyboardEnabled
                ? 'Bàn phím đã được bật'
                : 'Bàn phím chưa được bật'}
            </Text>
          </View>

          <Text style={styles.cardDesc}>
            Để gợi ý xuất hiện khi gõ ở mọi app (Zalo, Messenger, Gmail…), bạn
            cần bật bàn phím QuickText trong cài đặt hệ thống, rồi chuyển sang
            dùng khi cần.
          </Text>

          <TouchableOpacity
            style={styles.primaryButton}
            onPress={openKeyboardSettings}>
            <Text style={styles.primaryButtonText}>
              1. Mở cài đặt bàn phím
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.secondaryButton}
            onPress={showKeyboardPicker}>
            <Text style={styles.secondaryButtonText}>
              2. Chuyển sang QuickText
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.textButton}
            onPress={refreshKeyboardStatus}>
            <Text style={styles.textButtonLabel}>Kiểm tra lại trạng thái</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.guideCard}>
          <Text style={styles.guideTitle}>Cách sử dụng:</Text>
          <Text style={styles.guideText}>
            1. Nhấn "Mở cài đặt bàn phím" → tìm "QuickText" → bật công tắc{'\n'}
            2. Quay lại app, nhấn "Chuyển sang QuickText"{'\n'}
            3. Mở một app bất kỳ có ô nhập text (Zalo, Messenger…){'\n'}
            4. Chạm vào ô nhập text → bàn phím QuickText sẽ hiện lên{'\n'}
            5. Gõ phím tắt (vd: xc, cb, dc) → thanh gợi ý phía trên sẽ hiện câu
            đầy đủ{'\n'}
            6. Chạm vào gợi ý để chèn câu vào ô nhập text
          </Text>
        </View>
      </View>

      {/* BACKGROUND SERVICE SECTION */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Dịch vụ nền</Text>

        <View style={styles.settingRow}>
          <View style={styles.settingInfo}>
            <Text style={styles.settingLabel}>Chạy ngầm</Text>
            <Text style={styles.settingDesc}>
              Giữ app chạy nền với thông báo để truy cập nhanh
            </Text>
          </View>
          <Switch
            value={serviceRunning}
            onValueChange={handleToggleService}
            trackColor={{false: '#ddd', true: '#A3C9F1'}}
            thumbColor={serviceRunning ? '#4A90D9' : '#f4f3f4'}
          />
        </View>
      </View>

      {/* INFO SECTION */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Thông tin</Text>
        <View style={styles.infoRow}>
          <Text style={styles.infoLabel}>Phiên bản</Text>
          <Text style={styles.infoValue}>1.0.0</Text>
        </View>
        <View style={styles.infoRow}>
          <Text style={styles.infoLabel}>Ứng dụng</Text>
          <Text style={styles.infoValue}>QuickText</Text>
        </View>
      </View>

      <View style={{height: 32}} />
    </ScrollView>
  );
}

const styles = StyleSheet.create<any>({
  container: {
    flex: 1,
    backgroundColor: '#F5F7FA',
  },
  header: {
    paddingHorizontal: 20,
    paddingTop: 16,
    paddingBottom: 12,
  },
  title: {
    fontSize: 24,
    fontWeight: '800',
    color: '#1a1a1a',
  },
  section: {
    marginTop: 8,
    paddingHorizontal: 16,
  },
  sectionTitle: {
    fontSize: 13,
    fontWeight: '600',
    color: '#888',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    marginBottom: 8,
    paddingHorizontal: 4,
  },
  card: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
  },
  statusRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 10,
  },
  statusDot: (enabled: boolean) => ({
    width: 10,
    height: 10,
    borderRadius: 5,
    backgroundColor: enabled ? '#4CAF50' : '#E74C3C',
    marginRight: 8,
  }),
  statusText: {
    fontSize: 15,
    fontWeight: '600',
    color: '#333',
  },
  cardDesc: {
    fontSize: 14,
    color: '#666',
    lineHeight: 20,
    marginBottom: 14,
  },
  primaryButton: {
    backgroundColor: '#4A90D9',
    paddingVertical: 12,
    borderRadius: 10,
    alignItems: 'center',
    marginBottom: 8,
  },
  primaryButtonText: {
    color: '#fff',
    fontSize: 15,
    fontWeight: '600',
  },
  secondaryButton: {
    backgroundColor: '#E8F0FE',
    paddingVertical: 12,
    borderRadius: 10,
    alignItems: 'center',
    marginBottom: 8,
  },
  secondaryButtonText: {
    color: '#4A90D9',
    fontSize: 15,
    fontWeight: '600',
  },
  textButton: {
    paddingVertical: 10,
    alignItems: 'center',
  },
  textButtonLabel: {
    color: '#888',
    fontSize: 13,
  },
  guideCard: {
    backgroundColor: '#FFF9E6',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#FFE8A3',
  },
  guideTitle: {
    fontSize: 15,
    fontWeight: '700',
    color: '#8B6914',
    marginBottom: 8,
  },
  guideText: {
    fontSize: 14,
    color: '#6B5518',
    lineHeight: 24,
  },
  settingRow: {
    flexDirection: 'row',
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    alignItems: 'center',
    marginBottom: 12,
  },
  settingInfo: {
    flex: 1,
    marginRight: 12,
  },
  settingLabel: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
  },
  settingDesc: {
    fontSize: 13,
    color: '#888',
    marginTop: 4,
    lineHeight: 18,
  },
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 8,
  },
  infoLabel: {
    fontSize: 15,
    color: '#555',
  },
  infoValue: {
    fontSize: 15,
    color: '#333',
    fontWeight: '600',
  },
});
