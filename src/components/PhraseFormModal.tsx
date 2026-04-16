import React, {useState, useEffect} from 'react';
import {
  Modal,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import {Phrase} from '../types';

interface PhraseFormModalProps {
  visible: boolean;
  phrase?: Phrase | null;
  onSave: (data: {shortcut: string; text: string; category: string}) => void;
  onClose: () => void;
}

export function PhraseFormModal({
  visible,
  phrase,
  onSave,
  onClose,
}: PhraseFormModalProps) {
  const [shortcut, setShortcut] = useState('');
  const [text, setText] = useState('');
  const [category, setCategory] = useState('');

  useEffect(() => {
    if (phrase) {
      setShortcut(phrase.shortcut);
      setText(phrase.text);
      setCategory(phrase.category);
    } else {
      setShortcut('');
      setText('');
      setCategory('Chung');
    }
  }, [phrase, visible]);

  const handleSave = () => {
    if (shortcut.trim() && text.trim()) {
      onSave({
        shortcut: shortcut.trim(),
        text: text.trim(),
        category: category.trim() || 'Chung',
      });
      setShortcut('');
      setText('');
      setCategory('');
    }
  };

  return (
    <Modal visible={visible} transparent animationType="slide">
      <KeyboardAvoidingView
        style={styles.overlay}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <View style={styles.container}>
          <Text style={styles.title}>
            {phrase ? 'Sửa đoạn text' : 'Thêm đoạn text mới'}
          </Text>

          <Text style={styles.label}>Phím tắt</Text>
          <TextInput
            style={styles.input}
            value={shortcut}
            onChangeText={setShortcut}
            placeholder="VD: xc, cb, dc..."
            placeholderTextColor="#999"
            autoCapitalize="none"
          />

          <Text style={styles.label}>Nội dung</Text>
          <TextInput
            style={[styles.input, styles.textArea]}
            value={text}
            onChangeText={setText}
            placeholder="Nhập nội dung đoạn text..."
            placeholderTextColor="#999"
            multiline
            numberOfLines={4}
          />

          <Text style={styles.label}>Danh mục</Text>
          <TextInput
            style={styles.input}
            value={category}
            onChangeText={setCategory}
            placeholder="VD: Chào hỏi, Công việc..."
            placeholderTextColor="#999"
          />

          <View style={styles.buttons}>
            <TouchableOpacity
              style={[styles.button, styles.cancelButton]}
              onPress={onClose}>
              <Text style={styles.cancelText}>Hủy</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.button, styles.saveButton]}
              onPress={handleSave}>
              <Text style={styles.saveText}>Lưu</Text>
            </TouchableOpacity>
          </View>
        </View>
      </KeyboardAvoidingView>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    justifyContent: 'flex-end',
    backgroundColor: 'rgba(0,0,0,0.5)',
  },
  container: {
    backgroundColor: '#fff',
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    padding: 24,
  },
  title: {
    fontSize: 20,
    fontWeight: '700',
    color: '#1a1a1a',
    marginBottom: 20,
  },
  label: {
    fontSize: 14,
    fontWeight: '600',
    color: '#555',
    marginBottom: 6,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 12,
    paddingHorizontal: 14,
    paddingVertical: 10,
    fontSize: 16,
    color: '#333',
    backgroundColor: '#fafafa',
    marginBottom: 14,
  },
  textArea: {
    height: 100,
    textAlignVertical: 'top',
  },
  buttons: {
    flexDirection: 'row',
    gap: 12,
    marginTop: 8,
  },
  button: {
    flex: 1,
    paddingVertical: 14,
    borderRadius: 12,
    alignItems: 'center',
  },
  cancelButton: {
    backgroundColor: '#f0f0f0',
  },
  saveButton: {
    backgroundColor: '#4A90D9',
  },
  cancelText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#666',
  },
  saveText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#fff',
  },
});
