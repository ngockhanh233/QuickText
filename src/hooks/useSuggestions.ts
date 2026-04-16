import {useMemo} from 'react';
import {Phrase} from '../types';

export function useSuggestions(phrases: Phrase[], input: string): Phrase[] {
  return useMemo(() => {
    const trimmed = input.trim().toLowerCase();
    if (trimmed.length === 0) {
      return [];
    }

    // Extract the last word being typed
    const words = trimmed.split(/\s+/);
    const lastWord = words[words.length - 1];

    if (lastWord.length === 0) {
      return [];
    }

    // Match against shortcuts and text content
    const results = phrases
      .map(phrase => {
        const shortcutMatch = phrase.shortcut
          .toLowerCase()
          .startsWith(lastWord);
        const textMatch = phrase.text.toLowerCase().includes(lastWord);

        let score = 0;
        if (shortcutMatch) {
          // Exact shortcut match gets highest score
          score =
            phrase.shortcut.toLowerCase() === lastWord ? 100 : 50;
        } else if (textMatch) {
          score = 10;
        }

        return {phrase, score};
      })
      .filter(item => item.score > 0)
      .sort((a, b) => b.score - a.score)
      .map(item => item.phrase);

    return results.slice(0, 8);
  }, [phrases, input]);
}
