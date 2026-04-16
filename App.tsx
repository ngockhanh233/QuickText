import React from 'react';
import {StatusBar, StyleSheet, Text, useColorScheme, View} from 'react-native';
import {SafeAreaProvider} from 'react-native-safe-area-context';
import {NavigationContainer} from '@react-navigation/native';
import {createBottomTabNavigator} from '@react-navigation/bottom-tabs';
import {ComposeScreen} from './src/screens/ComposeScreen';
import {PhrasesScreen} from './src/screens/PhrasesScreen';
import {SettingsScreen} from './src/screens/SettingsScreen';
import {RootTabParamList} from './src/types';

const Tab = createBottomTabNavigator<RootTabParamList>();

function TabIcon({label, color}: {label: string; color: string}) {
  return (
    <View style={styles.iconContainer}>
      <Text style={[styles.iconText, {color}]}>{label}</Text>
    </View>
  );
}

function App() {
  const isDarkMode = useColorScheme() === 'dark';

  return (
    <SafeAreaProvider>
      <NavigationContainer>
        <StatusBar barStyle={isDarkMode ? 'light-content' : 'dark-content'} />
        <Tab.Navigator
          screenOptions={{
            headerShown: false,
            tabBarActiveTintColor: '#4A90D9',
            tabBarInactiveTintColor: '#999',
            tabBarStyle: {
              backgroundColor: '#fff',
              borderTopColor: '#eee',
              paddingBottom: 4,
              paddingTop: 4,
              height: 56,
            },
            tabBarLabelStyle: {
              fontSize: 12,
              fontWeight: '600',
            },
          }}>
          <Tab.Screen
            name="Compose"
            component={ComposeScreen}
            options={{
              tabBarLabel: 'Soạn',
              tabBarIcon: ({color}) => <TabIcon label="E" color={color} />,
            }}
          />
          <Tab.Screen
            name="Phrases"
            component={PhrasesScreen}
            options={{
              tabBarLabel: 'Quản lý',
              tabBarIcon: ({color}) => <TabIcon label="L" color={color} />,
            }}
          />
          <Tab.Screen
            name="Settings"
            component={SettingsScreen}
            options={{
              tabBarLabel: 'Cài đặt',
              tabBarIcon: ({color}) => <TabIcon label="G" color={color} />,
            }}
          />
        </Tab.Navigator>
      </NavigationContainer>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  iconContainer: {
    width: 24,
    height: 24,
    borderRadius: 12,
    backgroundColor: '#F0F0F0',
    justifyContent: 'center',
    alignItems: 'center',
  },
  iconText: {
    fontSize: 13,
    fontWeight: '800',
  },
});

export default App;
