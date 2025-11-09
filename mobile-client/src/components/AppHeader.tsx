import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Alert } from 'react-native';
import { useNavigation, useRoute, NavigationProp, RouteProp } from '@react-navigation/native';
import { useAuth } from '../navigation/AuthContext';
import { MainTabParamList } from '../types/navigation';

export default function AppHeader() {
  const navigation = useNavigation<NavigationProp<MainTabParamList>>();
  const route = useRoute<RouteProp<MainTabParamList>>();
  const { logout } = useAuth();

  const handleLogout = () => {
    Alert.alert('Logout', 'Are you sure you want to logout?', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Logout',
        style: 'destructive',
        onPress: async () => {
          await logout();
        },
      },
    ]);
  };

  const isActive = (screenName: keyof MainTabParamList) => {
    return route.name === screenName;
  };

  return (
    <View style={styles.container}>
      <View style={styles.leftSection}>
        <Text style={styles.appName}>Rapid Photo</Text>
      </View>
      
      <View style={styles.navigation}>
        <TouchableOpacity
          style={[styles.navButton, isActive('Upload') && styles.navButtonActive]}
          onPress={() => navigation.navigate('Upload')}
        >
          <Text style={[styles.navText, isActive('Upload') && styles.navTextActive]}>
            📷 Upload
          </Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.navButton, isActive('Gallery') && styles.navButtonActive]}
          onPress={() => navigation.navigate('Gallery')}
        >
          <Text style={[styles.navText, isActive('Gallery') && styles.navTextActive]}>
            🖼️ Gallery
          </Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.logoutButton} onPress={handleLogout}>
          <Text style={styles.logoutText}>Logout</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 15,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 3,
    elevation: 3,
  },
  leftSection: {
    flex: 1,
  },
  appName: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#007AFF',
  },
  navigation: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  navButton: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 8,
    backgroundColor: '#f5f5f5',
  },
  navButtonActive: {
    backgroundColor: '#007AFF',
  },
  navText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#666',
  },
  navTextActive: {
    color: '#fff',
  },
  logoutButton: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 8,
    backgroundColor: '#ff3b30',
    marginLeft: 5,
  },
  logoutText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#fff',
  },
});

