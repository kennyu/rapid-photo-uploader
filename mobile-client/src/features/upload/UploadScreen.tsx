import React, { useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  FlatList,
  Image,
  Alert,
  ActivityIndicator,
  SafeAreaView,
} from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import { apiClient } from '../../api/client';
import { UploadProgress } from '../../types/api';
import AppHeader from '../../components/AppHeader';

export default function UploadScreen() {
  const [uploads, setUploads] = useState<UploadProgress[]>([]);
  const [isUploading, setIsUploading] = useState(false);

  const pickImages = async () => {
    // Request permission
    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (status !== 'granted') {
      Alert.alert('Permission Denied', 'We need camera roll permissions to upload photos');
      return;
    }

    // Pick images (up to 100)
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images'],
      allowsMultipleSelection: true,
      quality: 0.8,
      selectionLimit: 100,
    });

    if (!result.canceled && result.assets) {
      await uploadImages(result.assets);
    }
  };

  const uploadImages = async (assets: ImagePicker.ImagePickerAsset[]) => {
    if (assets.length === 0) return;

    setIsUploading(true);

    // Initialize upload progress for all images
    const initialUploads: UploadProgress[] = assets.map((asset, index) => ({
      id: `upload-${Date.now()}-${index}`,
      filename: asset.uri.split('/').pop() || `image-${index}.jpg`,
      progress: 0,
      status: 'pending',
    }));

    setUploads(initialUploads);

    // Upload images concurrently
    const uploadPromises = assets.map(async (asset, index) => {
      try {
        // Use asset.fileName if available (native), otherwise generate from content type
        let filename = asset.fileName || asset.uri.split('/').pop();
        
        // Get file blob to determine actual size if not provided
        let fileSize = asset.fileSize;
        const contentType = asset.mimeType || 'image/jpeg';
        
        // If filename is just a UUID (from blob URL on web), generate proper filename
        if (!filename || !filename.includes('.')) {
          const extension = contentType.split('/')[1] || 'jpg'; // Extract extension from mime type
          filename = `image-${Date.now()}-${index}.${extension}`;
        }
        
        // If fileSize is not available or is 0, fetch the file to get its size
        if (!fileSize || fileSize === 0) {
          console.log(`[Upload ${index}] File size not available, fetching blob...`);
          const tempFile = await fetch(asset.uri);
          const tempBlob = await tempFile.blob();
          fileSize = tempBlob.size;
        }

        console.log(`[Upload ${index}] Starting upload for ${filename}`);
        console.log(`[Upload ${index}] File size: ${fileSize}, Content-Type: ${contentType}`);

        // Step 1: Initiate upload to get pre-signed URL
        setUploads((prev) =>
          prev.map((upload, i) =>
            i === index ? { ...upload, status: 'uploading' as const, progress: 10 } : upload
          )
        );

        console.log(`[Upload ${index}] Step 1: Initiating upload...`);
        const initiateResponse = await apiClient.post<{
          presignedUrl?: string;
          preSignedUrl?: string;
          photoId: string;
          uploadJobId: string;
        }>('/photos/upload/initiate', {
          filename,
          fileSize,
          contentType,
        });
        
        // Backend might return presignedUrl or preSignedUrl
        const presignedUrl = initiateResponse.presignedUrl || initiateResponse.preSignedUrl;
        
        if (!presignedUrl) {
          throw new Error('No presigned URL received from backend');
        }
        
        console.log(`[Upload ${index}] Step 1 ✓: Got presigned URL and uploadJobId: ${initiateResponse.uploadJobId}`);
        console.log(`[Upload ${index}] Presigned URL: ${presignedUrl.substring(0, 100)}...`);

        setUploads((prev) =>
          prev.map((upload, i) =>
            i === index ? { ...upload, progress: 30 } : upload
          )
        );

        // Step 2: Upload file directly to S3
        console.log(`[Upload ${index}] Step 2: Fetching file blob for upload...`);
        const file = await fetch(asset.uri);
        const blob = await file.blob();
        console.log(`[Upload ${index}] Step 2: Blob size: ${blob.size}, type: ${blob.type}`);
        
        // Validate blob size matches what we sent to backend
        if (blob.size !== fileSize) {
          console.warn(`[Upload ${index}] Blob size mismatch: expected ${fileSize}, got ${blob.size}`);
        }

        console.log(`[Upload ${index}] Step 2: Uploading to S3...`);
        const s3Response = await fetch(presignedUrl, {
          method: 'PUT',
          body: blob,
          headers: {
            'Content-Type': contentType,
          },
        });

        console.log(`[Upload ${index}] S3 Response Status: ${s3Response.status}`);
        console.log(`[Upload ${index}] S3 Response Headers:`, {
          etag: s3Response.headers.get('etag'),
          contentLength: s3Response.headers.get('content-length'),
        });

        if (!s3Response.ok) {
          const errorText = await s3Response.text();
          console.error(`[Upload ${index}] S3 Error Response:`, errorText);
          throw new Error(`S3 upload failed: ${s3Response.status} ${s3Response.statusText}`);
        }
        console.log(`[Upload ${index}] Step 2 ✓: S3 upload successful (${s3Response.status})`);

        setUploads((prev) =>
          prev.map((upload, i) =>
            i === index ? { ...upload, progress: 70 } : upload
          )
        );

        // Step 3: Notify backend that upload is complete
        console.log(`[Upload ${index}] Step 3: Notifying backend of completion...`);
        await apiClient.post(`/uploads/${initiateResponse.uploadJobId}/complete`, {});
        console.log(`[Upload ${index}] Step 3 ✓: Backend notified`);

        // Mark as complete
        setUploads((prev) =>
          prev.map((upload, i) =>
            i === index ? { ...upload, progress: 100, status: 'complete' as const } : upload
          )
        );
        console.log(`[Upload ${index}] ✅ Upload complete!`);
      } catch (error: any) {
        console.error(`[Upload ${index}] ❌ Upload failed:`, error);
        console.error(`[Upload ${index}] Error details:`, {
          message: error.message,
          response: error.response?.data,
          status: error.response?.status,
        });
        setUploads((prev) =>
          prev.map((upload, i) =>
            i === index
              ? {
                  ...upload,
                  status: 'error' as const,
                  error: error.response?.data?.message || error.message || 'Upload failed',
                }
              : upload
          )
        );
      }
    });

    await Promise.all(uploadPromises);
    setIsUploading(false);

    // Show completion message
    const successCount = uploads.filter((u) => u.status === 'complete').length;
    const failCount = uploads.filter((u) => u.status === 'error').length;

    Alert.alert(
      'Upload Complete',
      `${successCount} photos uploaded successfully${failCount > 0 ? `, ${failCount} failed` : ''}`
    );
  };

  const renderUploadItem = ({ item }: { item: UploadProgress }) => (
    <View style={styles.uploadItem}>
      <Text style={styles.filename} numberOfLines={1}>
        {item.filename}
      </Text>
      <View style={styles.progressContainer}>
        <View style={[styles.progressBar, { width: `${item.progress}%` }]} />
      </View>
      <Text style={styles.status}>{item.status === 'complete' ? '✓' : `${Math.round(item.progress)}%`}</Text>
      {item.error && <Text style={styles.error}>{item.error}</Text>}
    </View>
  );

  return (
    <SafeAreaView style={styles.safeArea}>
      <AppHeader />
      <View style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title}>Upload Photos</Text>
          <Text style={styles.subtitle}>Select up to 100 photos to upload</Text>
        </View>

      <TouchableOpacity
        style={[styles.button, isUploading && styles.buttonDisabled]}
        onPress={pickImages}
        disabled={isUploading}
      >
        {isUploading ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.buttonText}>📷 Select Photos</Text>
        )}
      </TouchableOpacity>

      {uploads.length > 0 && (
        <View style={styles.uploadsSection}>
          <Text style={styles.uploadsTitle}>
            Uploads ({uploads.filter((u) => u.status === 'complete').length}/{uploads.length})
          </Text>
          <FlatList
            data={uploads}
            renderItem={renderUploadItem}
            keyExtractor={(item) => item.id}
            style={styles.uploadsList}
          />
        </View>
      )}
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
  button: {
    backgroundColor: '#007AFF',
    margin: 20,
    padding: 18,
    borderRadius: 12,
    alignItems: 'center',
  },
  buttonDisabled: {
    opacity: 0.6,
  },
  buttonText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: '600',
  },
  uploadsSection: {
    flex: 1,
    margin: 20,
    marginTop: 0,
  },
  uploadsTitle: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 10,
    color: '#333',
  },
  uploadsList: {
    flex: 1,
  },
  uploadItem: {
    backgroundColor: '#fff',
    padding: 15,
    borderRadius: 8,
    marginBottom: 10,
  },
  filename: {
    fontSize: 14,
    fontWeight: '500',
    marginBottom: 8,
    color: '#333',
  },
  progressContainer: {
    height: 6,
    backgroundColor: '#e0e0e0',
    borderRadius: 3,
    overflow: 'hidden',
    marginBottom: 5,
  },
  progressBar: {
    height: '100%',
    backgroundColor: '#007AFF',
  },
  status: {
    fontSize: 12,
    color: '#666',
  },
  error: {
    fontSize: 12,
    color: '#ff3b30',
    marginTop: 5,
  },
});

