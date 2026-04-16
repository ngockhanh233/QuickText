import React from 'react';
import {
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import {Phrase} from '../types';

interface SuggestionBarProps {
  suggestions: Phrase[];
  onSelect: (phrase: Phrase) => void;
}

export function SuggestionBar({suggestions, onSelect}: SuggestionBarProps) {
  if (suggestions.length === 0) {
    return null;
  }

  return (
    <View style={styles.container}>
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        keyboardShouldPersistTaps="always"
        contentContainerStyle={styles.scrollContent}>
        {suggestions.map(phrase => (
          <TouchableOpacity
            key={phrase.id}
            style={styles.chip}
            onPress={() => onSelect(phrase)}
            activeOpacity={0.7}>
            <Text style={styles.shortcut}>{phrase.shortcut}</Text>
            <Text style={styles.text} numberOfLines={1}>
              {phrase.text}
            </Text>
          </TouchableOpacity>
        ))}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#f0f0f0',
    borderTopWidth: 1,
    borderTopColor: '#ddd',
    paddingVertical: 6,
  },
  scrollContent: {
    paddingHorizontal: 8,
    gap: 8,
  },
  chip: {
    backgroundColor: '#fff',
    borderRadius: 20,
    paddingHorizontal: 14,
    paddingVertical: 8,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 1},
    shadowOpacity: 0.15,
    shadowRadius: 2,
    maxWidth: 250,
  },
  shortcut: {
    fontSize: 12,
    fontWeight: '700',
    color: '#4A90D9',
    backgroundColor: '#E8F0FE',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
    overflow: 'hidden',
  },
  text: {
    fontSize: 14,
    color: '#333',
    flexShrink: 1,
  },
});
