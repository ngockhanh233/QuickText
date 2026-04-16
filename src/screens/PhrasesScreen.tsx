import React, {useState} from 'react';
import {
  Alert,
  FlatList,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import {usePhrases} from '../hooks/usePhrases';
import {PhraseListItem} from '../components/PhraseListItem';
import {PhraseFormModal} from '../components/PhraseFormModal';
import {Phrase} from '../types';

export function PhrasesScreen() {
  const {phrases, addPhrase, updatePhrase, deletePhrase} = usePhrases();
  const [modalVisible, setModalVisible] = useState(false);
  const [editingPhrase, setEditingPhrase] = useState<Phrase | null>(null);
  const [searchQuery, setSearchQuery] = useState('');

  const filteredPhrases = phrases.filter(p => {
    const q = searchQuery.toLowerCase();
    return (
      p.shortcut.toLowerCase().includes(q) ||
      p.text.toLowerCase().includes(q) ||
      p.category.toLowerCase().includes(q)
    );
  });

  const handleAdd = () => {
    setEditingPhrase(null);
    setModalVisible(true);
  };

  const handleEdit = (phrase: Phrase) => {
    setEditingPhrase(phrase);
    setModalVisible(true);
  };

  const handleDelete = (id: string) => {
    Alert.alert('Xóa đoạn text', 'Bạn có chắc muốn xóa đoạn text này?', [
      {text: 'Hủy', style: 'cancel'},
      {
        text: 'Xóa',
        style: 'destructive',
        onPress: () => deletePhrase(id),
      },
    ]);
  };

  const handleSave = async (data: {
    shortcut: string;
    text: string;
    category: string;
  }) => {
    if (editingPhrase) {
      await updatePhrase({
        ...editingPhrase,
        ...data,
        updatedAt: Date.now(),
      });
    } else {
      await addPhrase({
        id: Date.now().toString(),
        ...data,
        createdAt: Date.now(),
        updatedAt: Date.now(),
      });
    }
    setModalVisible(false);
    setEditingPhrase(null);
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Quản lý đoạn text</Text>
        <Text style={styles.count}>{phrases.length} đoạn text</Text>
      </View>

      <View style={styles.searchContainer}>
        <TextInput
          style={styles.searchInput}
          value={searchQuery}
          onChangeText={setSearchQuery}
          placeholder="Tìm kiếm phím tắt, nội dung..."
          placeholderTextColor="#aaa"
        />
      </View>

      <FlatList
        data={filteredPhrases}
        keyExtractor={item => item.id}
        renderItem={({item}) => (
          <PhraseListItem
            phrase={item}
            onPress={handleEdit}
            onDelete={handleDelete}
          />
        )}
        contentContainerStyle={styles.list}
        ListEmptyComponent={
          <View style={styles.empty}>
            <Text style={styles.emptyText}>
              {searchQuery
                ? 'Không tìm thấy kết quả'
                : 'Chưa có đoạn text nào'}
            </Text>
          </View>
        }
      />

      <TouchableOpacity style={styles.fab} onPress={handleAdd}>
        <Text style={styles.fabText}>+</Text>
      </TouchableOpacity>

      <PhraseFormModal
        visible={modalVisible}
        phrase={editingPhrase}
        onSave={handleSave}
        onClose={() => {
          setModalVisible(false);
          setEditingPhrase(null);
        }}
      />
    </View>
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
    paddingBottom: 8,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'baseline',
  },
  title: {
    fontSize: 24,
    fontWeight: '800',
    color: '#1a1a1a',
  },
  count: {
    fontSize: 14,
    color: '#888',
  },
  searchContainer: {
    paddingHorizontal: 16,
    paddingBottom: 8,
  },
  searchInput: {
    backgroundColor: '#fff',
    borderRadius: 12,
    paddingHorizontal: 16,
    paddingVertical: 10,
    fontSize: 15,
    color: '#333',
    elevation: 1,
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 1},
    shadowOpacity: 0.05,
    shadowRadius: 2,
  },
  list: {
    paddingTop: 4,
    paddingBottom: 80,
  },
  empty: {
    alignItems: 'center',
    paddingTop: 60,
  },
  emptyText: {
    fontSize: 16,
    color: '#aaa',
  },
  fab: {
    position: 'absolute',
    bottom: 24,
    right: 24,
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: '#4A90D9',
    justifyContent: 'center',
    alignItems: 'center',
    elevation: 6,
    shadowColor: '#4A90D9',
    shadowOffset: {width: 0, height: 4},
    shadowOpacity: 0.3,
    shadowRadius: 6,
  },
  fabText: {
    fontSize: 28,
    color: '#fff',
    fontWeight: '600',
    marginTop: -2,
  },
});
