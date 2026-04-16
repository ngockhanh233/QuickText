export interface Phrase {
  id: string;
  shortcut: string;
  text: string;
  category: string;
  createdAt: number;
  updatedAt: number;
}

export type RootTabParamList = {
  Compose: undefined;
  Phrases: undefined;
  Settings: undefined;
};
