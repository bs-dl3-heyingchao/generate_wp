<script setup lang="ts">
import { ref, computed } from 'vue'
import ScreenDesignTab from './components/ScreenDesignTab.vue'
import DBQueryTab from './components/DBQueryTab.vue'

const tab = ref(0)

const files1 = ref<File[]>([])
const files2 = ref<File[]>([])
const result1 = ref<'idle' | 'parsing' | 'success' | 'error'>('idle')
const result2 = ref<'idle' | 'parsing' | 'success' | 'error'>('idle')
const errors1 = ref<string[]>([])
const errors2 = ref<string[]>([])

const currentFiles = computed(() => tab.value === 0 ? files1.value : files2.value)
const currentResult = computed(() => tab.value === 0 ? result1.value : result2.value)
const currentErrors = computed(() => tab.value === 0 ? errors1.value : errors2.value)

const addFiles1 = (newFiles: File[]) => {
  files1.value = [...files1.value, ...newFiles]
}

const addFiles2 = (newFiles: File[]) => {
  files2.value = [...files2.value, ...newFiles]
}

const removeFile1 = (index: number) => {
  files1.value.splice(index, 1)
}

const removeFile2 = (index: number) => {
  files2.value.splice(index, 1)
}

const removeCurrentFile = (index: number) => {
  if (tab.value === 0) {
    removeFile1(index)
  } else {
    removeFile2(index)
  }
}

const generateCode1 = () => {
  result1.value = 'parsing'
  errors1.value = []
  // Simulate parsing
  setTimeout(() => {
    if (files1.value.length > 0) {
      result1.value = 'success'
    } else {
      result1.value = 'error'
      errors1.value = ['No files uploaded']
    }
  }, 2000)
}

const generateCode2 = () => {
  result2.value = 'parsing'
  errors2.value = []
  // Simulate parsing
  setTimeout(() => {
    if (files2.value.length > 0) {
      result2.value = 'success'
    } else {
      result2.value = 'error'
      errors2.value = ['No files uploaded']
    }
  }, 2000)
}

const generateCurrentCode = () => {
  if (tab.value === 0) {
    generateCode1()
  } else {
    generateCode2()
  }
}

const downloadZip = () => {
  // Simulate download
  alert('Downloading source.zip')
}
</script>

<template>
  <v-app>
    <v-main style="background-color: #f5f5f5;">
      <v-container fluid>
        <v-card class="mt-4" elevation="2">
          <v-card-title class="text-h5 pa-4">WP Tool Front</v-card-title>
          <v-card-text>
            <v-tabs v-model="tab" color="primary" centered class="mb-6">
              <v-tab value="screen">
                <v-icon left>mdi-file-document-outline</v-icon>
                画面设计书
              </v-tab>
              <v-tab value="db">
                <v-icon left>mdi-database</v-icon>
                DBQuery定义书
              </v-tab>
            </v-tabs>

            <v-window v-model="tab" class="mt-4">
              <v-window-item>
                <ScreenDesignTab @files-added="addFiles1" />
              </v-window-item>

              <v-window-item>
                <DBQueryTab @files-added="addFiles2" />
              </v-window-item>
            </v-window>

            <v-row class="mt-4">
              <v-col cols="12" md="8">
                <v-list v-if="currentFiles.length > 0">
                  <v-list-subheader>上传的文件</v-list-subheader>
                  <v-list-item v-for="(file, index) in currentFiles" :key="file.name + index">
                    <v-list-item-title>{{ file.name }}</v-list-item-title>
                    <template v-slot:append>
                      <v-btn icon="mdi-close" size="small" @click="removeCurrentFile(index)" color="error"></v-btn>
                    </template>
                  </v-list-item>
                </v-list>
              </v-col>
              <v-col cols="12" md="4">
                <v-btn
                  @click="generateCurrentCode"
                  color="primary"
                  size="large"
                  :disabled="currentFiles.length === 0"
                  block
                >
                  生成代码
                </v-btn>
              </v-col>
            </v-row>

            <v-card v-if="currentResult === 'parsing'" class="mt-4" color="blue-lighten-5">
              <v-card-text class="text-center">
                <v-progress-circular indeterminate color="primary"></v-progress-circular>
                <div class="mt-2">解析中...</div>
              </v-card-text>
            </v-card>

            <v-card v-if="currentResult === 'success'" class="mt-4" color="green-lighten-5">
              <v-card-text class="text-center">
                <v-icon color="green">mdi-check-circle</v-icon>
                <div class="mt-2">解析成功</div>
                <v-btn color="primary" @click="downloadZip" class="mt-2">
                  <v-icon left>mdi-download</v-icon>
                  下载 source.zip
                </v-btn>
              </v-card-text>
            </v-card>

            <v-card v-if="currentResult === 'error'" class="mt-4" color="red-lighten-5">
              <v-card-title class="text-error">错误一览</v-card-title>
              <v-card-text>
                <v-list>
                  <v-list-item v-for="error in currentErrors" :key="error">
                    <v-list-item-icon>
                      <v-icon color="error">mdi-alert-circle</v-icon>
                    </v-list-item-icon>
                    <v-list-item-title class="text-error">{{ error }}</v-list-item-title>
                  </v-list-item>
                </v-list>
              </v-card-text>
            </v-card>
          </v-card-text>
        </v-card>
      </v-container>
    </v-main>
  </v-app>
</template>

<style scoped>
</style>
