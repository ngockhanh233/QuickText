import {useState, useEffect, useCallback} from 'react';
import {Phrase} from '../types';
import {
  initializePhrases,
  getAllPhrases,
  savePhrase,
  deletePhrase as removePhrase,
} from '../storage/phraseStorage';

export function usePhrases() {
  const [phrases, setPhrases] = useState<Phrase[]>([]);
  const [loading, setLoading] = useState(true);

  const loadPhrases = useCallback(async () => {
    setLoading(true);
    const data = await getAllPhrases();
    setPhrases(data);
    setLoading(false);
  }, []);

  useEffect(() => {
    (async () => {
      await initializePhrases();
      await loadPhrases();
    })();
  }, [loadPhrases]);

  const addPhrase = useCallback(
    async (phrase: Phrase) => {
      await savePhrase(phrase);
      await loadPhrases();
    },
    [loadPhrases],
  );

  const updatePhrase = useCallback(
    async (phrase: Phrase) => {
      await savePhrase(phrase);
      await loadPhrases();
    },
    [loadPhrases],
  );

  const deletePhrase = useCallback(
    async (id: string) => {
      await removePhrase(id);
      await loadPhrases();
    },
    [loadPhrases],
  );

  return {phrases, loading, addPhrase, updatePhrase, deletePhrase, loadPhrases};
}
