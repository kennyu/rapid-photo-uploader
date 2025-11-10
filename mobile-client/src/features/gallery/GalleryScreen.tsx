import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  FlatList,
  Image,
  StyleSheet,
  RefreshControl,
  ActivityIndicator,
  Alert,
  SafeAreaView,
  TouchableOpacity,
  Modal,
  TextInput,
  ScrollView,
  Linking,
  Platform,
} from 'react-native';
import * as FileSystem from 'expo-file-system';
import * as MediaLibrary from 'expo-media-library';
import { apiClient } from '../../api/client';
import { Photo } from '../../types/api';
import AppHeader from '../../components/AppHeader';

export default function GalleryScreen() {
  const [photos, setPhotos] = useState<Photo[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [selectedPhoto, setSelectedPhoto] = useState<Photo | null>(null);
  const [modalVisible, setModalVisible] = useState(false);
  const [newTag, setNewTag] = useState('');
  const [isAddingTag, setIsAddingTag] = useState(false);
  const [isDownloading, setIsDownloading] = useState(false);
  const [selectedFilter, setSelectedFilter] = useState<string | null>(null);
  const [availableTags, setAvailableTags] = useState<string[]>([]);

  useEffect(() => {
    loadPhotos();
  }, [selectedFilter]);

  const loadPhotos = async () => {
    try {
      // Build query params
      const params = selectedFilter ? `?tag=${encodeURIComponent(selectedFilter)}` : '';
      
      const data = await apiClient.get<{
        content: Photo[];
        page: number;
        totalElements: number;
      }>(`/photos${params}`);
      
      setPhotos(data.content);
      
      // Extract all unique tags from photos (for filter UI)
      // Load all photos to get complete tag list
      if (!selectedFilter) {
        const allData = await apiClient.get<{
          content: Photo[];
        }>('/photos?size=1000'); // Get all photos to extract tags
        
        const tags = new Set<string>();
        allData.content.forEach((photo) => {
          photo.tags?.forEach((tag) => tags.add(tag));
        });
        setAvailableTags(Array.from(tags).sort());
      }
    } catch (error: any) {
      console.error('Error loading photos:', error);
      Alert.alert('Error', 'Failed to load photos');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  const handleRefresh = () => {
    setRefreshing(true);
    loadPhotos();
  };

  const openPhotoModal = (photo: Photo) => {
    setSelectedPhoto(photo);
    setModalVisible(true);
  };

  const closeModal = () => {
    setModalVisible(false);
    setSelectedPhoto(null);
    setNewTag('');
  };

  const handleAddTag = async () => {
    if (!selectedPhoto || !newTag.trim()) return;

    setIsAddingTag(true);
    try {
      await apiClient.post(`/photos/${selectedPhoto.id}/tags/${encodeURIComponent(newTag.trim())}`);
      
      // Update local state
      const updatedPhotos = photos.map((p) =>
        p.id === selectedPhoto.id ? { ...p, tags: [...p.tags, newTag.trim()] } : p
      );
      setPhotos(updatedPhotos);
      setSelectedPhoto({ ...selectedPhoto, tags: [...selectedPhoto.tags, newTag.trim()] });
      setNewTag('');
      Alert.alert('Success', 'Tag added successfully');
    } catch (error: any) {
      console.error('Error adding tag:', error);
      Alert.alert('Error', 'Failed to add tag');
    } finally {
      setIsAddingTag(false);
    }
  };

  const handleRemoveTag = async (tag: string) => {
    if (!selectedPhoto) return;

    try {
      await apiClient.delete(`/photos/${selectedPhoto.id}/tags/${encodeURIComponent(tag)}`);
      
      // Update local state
      const updatedPhotos = photos.map((p) =>
        p.id === selectedPhoto.id ? { ...p, tags: p.tags.filter((t) => t !== tag) } : p
      );
      setPhotos(updatedPhotos);
      setSelectedPhoto({ ...selectedPhoto, tags: selectedPhoto.tags.filter((t) => t !== tag) });
      Alert.alert('Success', 'Tag removed successfully');
    } catch (error: any) {
      console.error('Error removing tag:', error);
      Alert.alert('Error', 'Failed to remove tag');
    }
  };

  const handleDownload = async () => {
    if (!selectedPhoto || !selectedPhoto.downloadUrl) {
      Alert.alert('Error', 'Download URL not available');
      return;
    }

    setIsDownloading(true);

    try {
      if (Platform.OS === 'web') {
        // For web, open download URL in new tab
        Linking.openURL(selectedPhoto.downloadUrl);
        Alert.alert('Success', 'Download started in new tab');
      } else {
        // For native, download to device
        const { status } = await MediaLibrary.requestPermissionsAsync();
        if (status !== 'granted') {
          Alert.alert('Permission Denied', 'Media library access is required to save photos');
          return;
        }

        const fileUri = FileSystem.documentDirectory + selectedPhoto.filename;
        const downloadResumable = FileSystem.createDownloadResumable(
          selectedPhoto.downloadUrl,
          fileUri
        );

        const result = await downloadResumable.downloadAsync();
        if (result) {
          await MediaLibrary.createAssetAsync(result.uri);
          Alert.alert('Success', `Photo saved to gallery: ${selectedPhoto.filename}`);
        }
      }
    } catch (error: any) {
      console.error('Error downloading photo:', error);
      Alert.alert('Error', 'Failed to download photo');
    } finally {
      setIsDownloading(false);
    }
  };

  const renderPhoto = ({ item }: { item: Photo }) => (
    <TouchableOpacity style={styles.photoItem} onPress={() => openPhotoModal(item)} activeOpacity={0.7}>
      {item.thumbnailUrl ? (
        <Image source={{ uri: item.thumbnailUrl }} style={styles.photo} resizeMode="cover" />
      ) : (
        <View style={[styles.photo, styles.photoPlaceholder]}>
          <Text style={styles.placeholderText}>📷</Text>
        </View>
      )}
      <View style={styles.photoInfo}>
        <Text style={styles.filename} numberOfLines={1}>
          {item.filename}
        </Text>
        <Text style={styles.status}>{item.status}</Text>
        {item.tags && item.tags.length > 0 && (
          <Text style={styles.tags} numberOfLines={1}>
            🏷️ {item.tags.join(', ')}
          </Text>
        )}
      </View>
    </TouchableOpacity>
  );

  if (loading) {
    return (
      <View style={styles.centerContainer}>
        <ActivityIndicator size="large" color="#007AFF" />
      </View>
    );
  }

  return (
    <SafeAreaView style={styles.safeArea}>
      <AppHeader />
      <View style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title}>Gallery</Text>
          <Text style={styles.subtitle}>
            {photos.length} photos{selectedFilter ? ` with tag "${selectedFilter}"` : ''}
          </Text>
        </View>

        {/* Tag Filter */}
        {availableTags.length > 0 && (
          <View style={styles.filterContainer}>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.filterScroll}>
              <TouchableOpacity
                style={[styles.filterChip, !selectedFilter && styles.filterChipActive]}
                onPress={() => setSelectedFilter(null)}
              >
                <Text style={[styles.filterChipText, !selectedFilter && styles.filterChipTextActive]}>
                  All
                </Text>
              </TouchableOpacity>
              {availableTags.map((tag) => (
                <TouchableOpacity
                  key={tag}
                  style={[styles.filterChip, selectedFilter === tag && styles.filterChipActive]}
                  onPress={() => setSelectedFilter(tag)}
                >
                  <Text style={[styles.filterChipText, selectedFilter === tag && styles.filterChipTextActive]}>
                    {tag}
                  </Text>
                </TouchableOpacity>
              ))}
            </ScrollView>
          </View>
        )}

      {photos.length === 0 ? (
        <View style={styles.centerContainer}>
          <Text style={styles.emptyText}>📷</Text>
          <Text style={styles.emptySubtext}>
            {selectedFilter ? `No photos with tag "${selectedFilter}"` : 'No photos yet'}
          </Text>
          <Text style={styles.emptyHint}>
            {selectedFilter ? 'Try selecting a different tag' : 'Upload some photos to see them here'}
          </Text>
        </View>
      ) : (
        <FlatList
          data={photos}
          renderItem={renderPhoto}
          keyExtractor={(item) => item.id}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />}
          contentContainerStyle={styles.listContent}
        />
      )}

      {/* Photo Detail Modal */}
      <Modal
        visible={modalVisible}
        animationType="slide"
        transparent={true}
        onRequestClose={closeModal}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <ScrollView>
              {/* Header */}
              <View style={styles.modalHeader}>
                <Text style={styles.modalTitle}>Photo Details</Text>
                <TouchableOpacity onPress={closeModal} style={styles.closeButton}>
                  <Text style={styles.closeButtonText}>✕</Text>
                </TouchableOpacity>
              </View>

              {/* Photo Preview */}
              {selectedPhoto && (
                <>
                  {selectedPhoto.thumbnailUrl ? (
                    <Image
                      source={{ uri: selectedPhoto.thumbnailUrl }}
                      style={styles.modalPhoto}
                      resizeMode="contain"
                    />
                  ) : (
                    <View style={[styles.modalPhoto, styles.photoPlaceholder]}>
                      <Text style={styles.placeholderText}>📷</Text>
                    </View>
                  )}

                  {/* File Info */}
                  <View style={styles.infoSection}>
                    <Text style={styles.infoLabel}>Filename:</Text>
                    <Text style={styles.infoValue}>{selectedPhoto.filename}</Text>
                    <Text style={styles.infoLabel}>Status:</Text>
                    <Text style={styles.infoValue}>{selectedPhoto.status}</Text>
                    <Text style={styles.infoLabel}>Size:</Text>
                    <Text style={styles.infoValue}>
                      {(selectedPhoto.fileSize / 1024 / 1024).toFixed(2)} MB
                    </Text>
                  </View>

                  {/* Tags Section */}
                  <View style={styles.tagsSection}>
                    <Text style={styles.sectionTitle}>Tags</Text>
                    <View style={styles.tagsList}>
                      {selectedPhoto.tags && selectedPhoto.tags.length > 0 ? (
                        selectedPhoto.tags.map((tag, index) => (
                          <View key={index} style={styles.tagChip}>
                            <Text style={styles.tagText}>{tag}</Text>
                            <TouchableOpacity onPress={() => handleRemoveTag(tag)}>
                              <Text style={styles.tagRemove}>✕</Text>
                            </TouchableOpacity>
                          </View>
                        ))
                      ) : (
                        <Text style={styles.noTagsText}>No tags yet</Text>
                      )}
                    </View>

                    {/* Add Tag Input */}
                    <View style={styles.addTagContainer}>
                      <TextInput
                        style={styles.tagInput}
                        placeholder="Add a tag..."
                        value={newTag}
                        onChangeText={setNewTag}
                        onSubmitEditing={handleAddTag}
                        returnKeyType="done"
                      />
                      <TouchableOpacity
                        style={[styles.addTagButton, isAddingTag && styles.buttonDisabled]}
                        onPress={handleAddTag}
                        disabled={isAddingTag || !newTag.trim()}
                      >
                        {isAddingTag ? (
                          <ActivityIndicator size="small" color="#fff" />
                        ) : (
                          <Text style={styles.addTagButtonText}>Add</Text>
                        )}
                      </TouchableOpacity>
                    </View>
                  </View>

                  {/* Download Button */}
                  <TouchableOpacity
                    style={[styles.downloadButton, isDownloading && styles.buttonDisabled]}
                    onPress={handleDownload}
                    disabled={isDownloading || !selectedPhoto.downloadUrl}
                  >
                    {isDownloading ? (
                      <ActivityIndicator size="small" color="#fff" />
                    ) : (
                      <Text style={styles.downloadButtonText}>📥 Download Photo</Text>
                    )}
                  </TouchableOpacity>
                </>
              )}
            </ScrollView>
          </View>
        </View>
      </Modal>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#fff',
  },
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  header: {
    padding: 20,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#ddd',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#333',
  },
  subtitle: {
    fontSize: 14,
    color: '#666',
    marginTop: 5,
  },
  centerContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  emptyText: {
    fontSize: 64,
    marginBottom: 10,
  },
  emptySubtext: {
    fontSize: 18,
    fontWeight: '600',
    color: '#333',
    marginBottom: 5,
  },
  emptyHint: {
    fontSize: 14,
    color: '#666',
  },
  listContent: {
    padding: 10,
  },
  photoItem: {
    backgroundColor: '#fff',
    borderRadius: 12,
    marginBottom: 10,
    padding: 10,
    flexDirection: 'row',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 2,
  },
  photo: {
    width: 80,
    height: 80,
    borderRadius: 8,
    marginRight: 12,
  },
  photoPlaceholder: {
    backgroundColor: '#e0e0e0',
    justifyContent: 'center',
    alignItems: 'center',
  },
  placeholderText: {
    fontSize: 32,
  },
  photoInfo: {
    flex: 1,
  },
  filename: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    marginBottom: 4,
  },
  status: {
    fontSize: 12,
    color: '#007AFF',
    marginBottom: 4,
  },
  tags: {
    fontSize: 12,
    color: '#666',
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  modalContent: {
    width: '90%',
    maxHeight: '80%',
    backgroundColor: '#fff',
    borderRadius: 15,
    overflow: 'hidden',
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 20,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  modalTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
  },
  closeButton: {
    width: 30,
    height: 30,
    justifyContent: 'center',
    alignItems: 'center',
  },
  closeButtonText: {
    fontSize: 24,
    color: '#666',
  },
  modalPhoto: {
    width: '100%',
    height: 250,
    backgroundColor: '#f0f0f0',
  },
  infoSection: {
    padding: 20,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  infoLabel: {
    fontSize: 12,
    color: '#666',
    marginTop: 8,
  },
  infoValue: {
    fontSize: 16,
    color: '#333',
    fontWeight: '500',
    marginBottom: 8,
  },
  tagsSection: {
    padding: 20,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 12,
  },
  tagsList: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginBottom: 15,
  },
  tagChip: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#007AFF',
    borderRadius: 16,
    paddingVertical: 6,
    paddingHorizontal: 12,
    marginRight: 8,
    marginBottom: 8,
  },
  tagText: {
    color: '#fff',
    fontSize: 14,
    marginRight: 6,
  },
  tagRemove: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
  noTagsText: {
    fontSize: 14,
    color: '#999',
    fontStyle: 'italic',
  },
  addTagContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  tagInput: {
    flex: 1,
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 16,
    marginRight: 10,
  },
  addTagButton: {
    backgroundColor: '#007AFF',
    borderRadius: 8,
    paddingHorizontal: 20,
    paddingVertical: 10,
    minWidth: 60,
    alignItems: 'center',
  },
  addTagButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  downloadButton: {
    backgroundColor: '#28a745',
    borderRadius: 10,
    padding: 15,
    margin: 20,
    marginTop: 10,
    alignItems: 'center',
  },
  downloadButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
  buttonDisabled: {
    opacity: 0.5,
  },
  filterContainer: {
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
    paddingVertical: 10,
  },
  filterScroll: {
    paddingHorizontal: 15,
  },
  filterChip: {
    backgroundColor: '#f0f0f0',
    borderRadius: 20,
    paddingVertical: 8,
    paddingHorizontal: 16,
    marginRight: 8,
    borderWidth: 1,
    borderColor: '#ddd',
  },
  filterChipActive: {
    backgroundColor: '#007AFF',
    borderColor: '#007AFF',
  },
  filterChipText: {
    color: '#666',
    fontSize: 14,
    fontWeight: '500',
  },
  filterChipTextActive: {
    color: '#fff',
    fontWeight: '600',
  },
});

