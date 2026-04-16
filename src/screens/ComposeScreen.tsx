import React, {useState, useRef} from 'react';
import {
  StyleSheet,
  Text,
  TextInput,
  View,
  KeyboardAvoidingView,
  Platform,
  TouchableOpacity,
  Alert,
} from 'react-native';
import Clipboard from '@react-native-clipboard/clipboard';
import {usePhrases} from '../hooks/usePhrases';
import {useSuggestions} from '../hooks/useSuggestions';
import {SuggestionBar} from '../components/SuggestionBar';
import {Phrase} from '../types';

export function ComposeScreen() {
  const [inputText, setInputText] = useState('');
  const inputRef = useRef<TextInput>(null);
  const {phrases} = usePhrases();
  const suggestions = useSuggestions(phrases, inputText);

  const handleSelectSuggestion = (phrase: Phrase) => {
    // Replace the last word with the phrase text
    const words = inputText.split(/\s+/);
    words[words.length - 1] = phrase.text;
    const newText = words.join(' ') + ' ';
    setInputText(newText);
    inputRef.current?.focus();
  };

  const handleCopy = () => {
    if (inputText.trim()) {
      Clipboard.setString(inputText.trim());
      Alert.alert('Copied!', 'Đã sao chép nội dung vào clipboard.');
    }
  };

  const handleClear = () => {
    setInputText('');
    inputRef.current?.focus();
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      keyboardVerticalOffset={Platform.OS === 'ios' ? 90 : 0}>
      <View style={styles.header}>
        <Text style={styles.title}>QuickText</Text>
        <Text style={styles.subtitle}>
          Gõ phím tắt để xem gợi ý bên dưới
        </Text>
      </View>

      <View style={styles.inputContainer}>
        <TextInput
          ref={inputRef}
          style={styles.textInput}
          value={inputText}
          onChangeText={setInputText}
          placeholder="Bắt đầu gõ... (VD: xc, cb, dc)"
          placeholderTextColor="#aaa"
          multiline
          autoFocus
        />
        <View style={styles.actions}>
          <TouchableOpacity style={styles.actionButton} onPress={handleCopy}>
            <Text style={styles.actionText}>Sao chép</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.actionButton, styles.clearButton]}
            onPress={handleClear}>
            <Text style={[styles.actionText, styles.clearText]}>Xóa</Text>
          </TouchableOpacity>
        </View>
      </View>

      <View style={styles.hintContainer}>
        <Text style={styles.hintTitle}>Phím tắt có sẵn:</Text>
        <View style={styles.hintChips}>
          {phrases.slice(0, 6).map(p => (
            <TouchableOpacity
              key={p.id}
              style={styles.hintChip}
              onPress={() => {
                setInputText(prev => prev + p.shortcut);
                inputRef.current?.focus();
              }}>
              <Text style={styles.hintChipShortcut}>{p.shortcut}</Text>
              <Text style={styles.hintChipText} numberOfLines={1}>
                {p.text}
              </Text>
            </TouchableOpacity>
          ))}
        </View>
      </View>

      <View style={styles.suggestionWrapper}>
        <SuggestionBar
          suggestions={suggestions}
          onSelect={handleSelectSuggestion}
        />
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
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
    fontSize: 28,
    fontWeight: '800',
    color: '#1a1a1a',
  },
  subtitle: {
    fontSize: 14,
    color: '#888',
    marginTop: 4,
  },
  inputContainer: {
    marginHorizontal: 16,
    backgroundColor: '#fff',
    borderRadius: 16,
    padding: 16,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 2},
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  textInput: {
    fontSize: 17,
    color: '#333',
    minHeight: 120,
    textAlignVertical: 'top',
    lineHeight: 24,
  },
  actions: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    gap: 10,
    marginTop: 10,
    borderTopWidth: 1,
    borderTopColor: '#f0f0f0',
    paddingTop: 10,
  },
  actionButton: {
    backgroundColor: '#4A90D9',
    paddingHorizontal: 18,
    paddingVertical: 8,
    borderRadius: 8,
  },
  clearButton: {
    backgroundColor: '#f0f0f0',
  },
  actionText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
  },
  clearText: {
    color: '#666',
  },
  hintContainer: {
    paddingHorizontal: 20,
    paddingTop: 20,
  },
  hintTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#888',
    marginBottom: 10,
  },
  hintChips: {
    gap: 8,
  },
  hintChip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    backgroundColor: '#fff',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 10,
  },
  hintChipShortcut: {
    fontSize: 13,
    fontWeight: '700',
    color: '#4A90D9',
    backgroundColor: '#E8F0FE',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 5,
    overflow: 'hidden',
  },
  hintChipText: {
    fontSize: 14,
    color: '#555',
    flex: 1,
  },
  suggestionWrapper: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
  },
});
