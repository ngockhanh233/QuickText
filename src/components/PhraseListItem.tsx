import React from 'react';
import {StyleSheet, Text, TouchableOpacity, View} from 'react-native';
import {Phrase} from '../types';

interface PhraseListItemProps {
  phrase: Phrase;
  onPress: (phrase: Phrase) => void;
  onDelete: (id: string) => void;
}

export function PhraseListItem({phrase, onPress, onDelete}: PhraseListItemProps) {
  return (
    <TouchableOpacity
      style={styles.container}
      onPress={() => onPress(phrase)}
      activeOpacity={0.7}>
      <View style={styles.content}>
        <View style={styles.header}>
          <View style={styles.shortcutBadge}>
            <Text style={styles.shortcutText}>{phrase.shortcut}</Text>
          </View>
          <Text style={styles.category}>{phrase.category}</Text>
        </View>
        <Text style={styles.text} numberOfLines={2}>
          {phrase.text}
        </Text>
      </View>
      <TouchableOpacity
        style={styles.deleteButton}
        onPress={() => onDelete(phrase.id)}
        hitSlop={{top: 10, bottom: 10, left: 10, right: 10}}>
        <Text style={styles.deleteText}>Xóa</Text>
      </TouchableOpacity>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    backgroundColor: '#fff',
    marginHorizontal: 16,
    marginVertical: 4,
    borderRadius: 12,
    padding: 14,
    alignItems: 'center',
    elevation: 1,
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 1},
    shadowOpacity: 0.1,
    shadowRadius: 2,
  },
  content: {
    flex: 1,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 6,
  },
  shortcutBadge: {
    backgroundColor: '#E8F0FE',
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 6,
  },
  shortcutText: {
    fontSize: 13,
    fontWeight: '700',
    color: '#4A90D9',
  },
  category: {
    fontSize: 12,
    color: '#999',
  },
  text: {
    fontSize: 15,
    color: '#444',
    lineHeight: 20,
  },
  deleteButton: {
    paddingHorizontal: 12,
    paddingVertical: 6,
  },
  deleteText: {
    fontSize: 14,
    color: '#E74C3C',
    fontWeight: '600',
  },
});
