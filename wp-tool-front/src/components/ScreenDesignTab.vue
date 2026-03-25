<template>
  <div class="upload-container">
    <v-card
      outlined
      class="upload-card pa-6"
      elevation="3"
    >
      <div
        @drop="onDrop"
        @dragover="onDragOver"
        class="upload-area text-center py-12"
      >
        <v-icon size="64" color="primary" class="mb-4">mdi-cloud-upload</v-icon>
        <div class="text-h5 mb-2">ファイルをここにドラッグしてアップロード</div>
        <div class="text-subtitle-1 text-medium-emphasis mb-6">複数のファイルに対応</div>
        <v-btn
          color="primary"
          variant="elevated"
          size="large"
          class="upload-btn"
          @click="$refs.fileInput.click()"
        >
          <v-icon left>mdi-file-plus</v-icon>
          ファイルを選択
        </v-btn>
        <input
          ref="fileInput"
          type="file"
          multiple
          style="display: none;"
          @change="onFileSelect"
        />
      </div>
    </v-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const emit = defineEmits<{
  filesAdded: [files: File[]]
}>()

const onDrop = (event: DragEvent) => {
  event.preventDefault()
  if (event.dataTransfer?.files) {
    emit('filesAdded', Array.from(event.dataTransfer.files))
  }
}

const onDragOver = (event: DragEvent) => {
  event.preventDefault()
}

const onFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files) {
    emit('filesAdded', Array.from(target.files))
  }
}
</script>

<style scoped>
.upload-container {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.upload-card {
  width: 50vw;
  min-width: 500px;
  border: 2px dashed #1976d2 !important;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.upload-card:hover {
  border-color: #0d47a1 !important;
  box-shadow: 0 8px 25px rgba(25, 118, 210, 0.15) !important;
}

.upload-area {
  min-height: 200px;
}

.upload-btn {
  transition: all 0.2s ease;
}

.upload-btn:hover {
  transform: translateY(-2px);
}
</style>