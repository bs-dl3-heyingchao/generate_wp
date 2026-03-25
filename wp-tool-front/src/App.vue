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

const generateCode1 = () => {
  result1.value = 'parsing'
  errors1.value = []
  // Simulate parsing
  setTimeout(() => {
    if (files1.value.length > 0) {
      result1.value = 'success'
    } else {
      result1.value = 'error'
      errors1.value = ['ファイルがアップロードされていません']
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
      errors2.value = ['ファイルがアップロードされていません']
    }
  }, 2000)
}

const downloadZip = () => {
  // Simulate download
  alert('source.zip をダウンロード中')
}

const clearFiles1 = () => {
  files1.value = []
  result1.value = 'idle'
  errors1.value = []
}

const templateScreenName = '（内部設計書サンプル_カード）画面設計書.xlsx'
const templateDBQueryName = '（内部設計書サンプル_カード）dbQuery定義書.xlsx'

const clearFiles2 = () => {
  files2.value = []
  result2.value = 'idle'
  errors2.value = []
}
</script>

<template>
  <v-app>
    <v-main style="background-color: #f5f5f5;">
      <v-container fluid>
        <v-card class="mt-4" elevation="3">
          <v-card-title class="text-h5 pa-4" style="background-color: #8E2DE2; color: white;">WP内部設計仕様書解析ツール</v-card-title>
          <v-card-text>
            <div style="background-color: #8E2DE2; padding: 10px 0 10px 0; border-radius: 0 0 8px 8px; margin: -20px -20px 0 -20px;">
              <v-tabs v-model="tab" style="background-color: #8E2DE2; padding: 0 20px;">
                <v-tab style="background-color: rgba(255, 255, 255, 0.2); color: white; margin-right: 10px; border-radius: 8px 8px 0 0;">画面仕様書</v-tab>
                <v-tab style="background-color: rgba(255, 255, 255, 0.2); color: white; margin-right: 10px; border-radius: 8px 8px 0 0;">DBQuery定義書</v-tab>
              </v-tabs>
            </div>

            <v-window v-model="tab">
              <v-window-item>
                <!-- 模版区域 -->
                <v-card flat class="mb-4" elevation="2" rounded="lg" outlined>
                  <v-card-title class="text-subtitle-1">テンプレート</v-card-title>
                  <v-card-text>
                    <div class="d-flex justify-end">
                      <a :href="`/templete/screen/${templateScreenName}`" download style="text-decoration: none; color: #1976d2;">
                        {{ templateScreenName }}
                      </a>
                    </div>
                  </v-card-text>
                </v-card>

                <!-- 上传区域 -->
                <v-card flat class="mb-4" elevation="2" rounded="lg" outlined>
                  <v-card-title class="text-subtitle-1">アップロード</v-card-title>
                  <v-card-text>
                    <ScreenDesignTab @files-added="addFiles1" />
                    <v-list v-if="files1.length > 0" class="mt-4">
                      <v-list-subheader>アップロードされたファイル</v-list-subheader>
                      <v-list-item v-for="(file, index) in files1" :key="file.name + index">
                        <v-list-item-title>{{ file.name }}</v-list-item-title>
                        <template v-slot:append>
                          <v-btn icon="mdi-close" size="small" @click="removeFile1(index)" color="error"></v-btn>
                        </template>
                      </v-list-item>
                    </v-list>
                    <div class="d-flex justify-end mt-4">
                      <v-btn
                        @click="generateCode1"
                        color="primary"
                        class="action-btn mr-2"
                        :disabled="files1.length === 0"
                      >
                        コード生成
                      </v-btn>
                      <v-btn v-if="files1.length > 0" color="warning" class="action-btn" @click="clearFiles1">
                        クリア
                      </v-btn>
                    </div>
                  </v-card-text>
                </v-card>

                <!-- 结果区域 -->
                <v-card v-if="result1 !== 'idle'" flat class="mb-4" elevation="2" rounded="lg" outlined>
                  <v-card-title class="text-subtitle-1">結果</v-card-title>
                  <v-card-text>
                    <v-card v-if="result1 === 'parsing'" class="mt-4" color="blue-lighten-5">
                      <v-card-text class="text-center">
                        <v-progress-circular indeterminate color="primary"></v-progress-circular>
                        <div class="mt-2">解析中...</div>
                      </v-card-text>
                    </v-card>
                    <v-card v-if="result1 === 'success'" class="mt-4" color="green-lighten-5">
                      <v-card-text class="text-center">
                        <v-icon color="green">mdi-check-circle</v-icon>
                        <div class="mt-2">解析成功</div>
                        <v-btn color="primary" @click="downloadZip" class="mt-2">
                          <v-icon left>mdi-download</v-icon>
                          SOURCE.ZIP をダウンロード
                        </v-btn>
                      </v-card-text>
                    </v-card>
                  </v-card-text>
                </v-card>

                <!-- Error区域 -->
                <v-card v-if="result1 === 'error'" flat class="mb-4" elevation="2" rounded="lg" outlined>
                  <v-card-title class="text-subtitle-1">エラー</v-card-title>
                  <v-card-text>
                    <v-card color="red-lighten-5">
                      <v-card-title class="text-error">エラーメッセージ一覧</v-card-title>
                      <v-card-text>
                        <v-list>
                          <v-list-item v-for="error in errors1" :key="error">
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
              </v-window-item>

              <v-window-item>
                <!-- 模版区域 -->
                <v-card flat class="mb-4" elevation="2" rounded="lg" outlined>
                  <v-card-title class="text-subtitle-1">テンプレート</v-card-title>
                  <v-card-text>
                    <div class="d-flex justify-end">
                      <a :href="`/templete/dbquery/${templateDBQueryName}`" download style="text-decoration: none; color: #1976d2;">
                        {{ templateDBQueryName }}
                      </a>
                    </div>
                  </v-card-text>
                </v-card>

                <!-- 上传区域 -->
                <v-card flat class="mb-4" elevation="2" rounded="lg" outlined>
                  <v-card-title class="text-subtitle-1">アップロード</v-card-title>
                  <v-card-text>
                    <DBQueryTab @files-added="addFiles2" />
                    <v-list v-if="files2.length > 0" class="mt-4">
                      <v-list-subheader>アップロードされたファイル</v-list-subheader>
                      <v-list-item v-for="(file, index) in files2" :key="file.name + index">
                        <v-list-item-title>{{ file.name }}</v-list-item-title>
                        <template v-slot:append>
                          <v-btn icon="mdi-close" size="small" @click="removeFile2(index)" color="error"></v-btn>
                        </template>
                      </v-list-item>
                    </v-list>
                    <div class="d-flex justify-end mt-4">
                      <v-btn
                        @click="generateCode2"
                        color="primary"
                        class="action-btn mr-2"
                        :disabled="files2.length === 0"
                      >
                        コード生成
                      </v-btn>
                      <v-btn v-if="files2.length > 0" color="warning" class="action-btn" @click="clearFiles2">
                        クリア
                      </v-btn>
                    </div>
                  </v-card-text>
                </v-card>

                <!-- 结果区域 -->
                <v-card v-if="result2 !== 'idle'" flat class="mb-4" elevation="2" rounded="lg" outlined>
                  <v-card-title class="text-subtitle-1">結果</v-card-title>
                  <v-card-text>
                    <v-card v-if="result2 === 'parsing'" class="mt-4" color="blue-lighten-5">
                      <v-card-text class="text-center">
                        <v-progress-circular indeterminate color="primary"></v-progress-circular>
                        <div class="mt-2">解析中...</div>
                      </v-card-text>
                    </v-card>
                    <v-card v-if="result2 === 'success'" class="mt-4" color="green-lighten-5">
                      <v-card-text class="text-center">
                        <v-icon color="green">mdi-check-circle</v-icon>
                        <div class="mt-2">解析成功</div>
                        <v-btn color="primary" @click="downloadZip" class="mt-2">
                          <v-icon left>mdi-download</v-icon>
                          SOURCE.ZIP をダウンロード
                        </v-btn>
                      </v-card-text>
                    </v-card>
                  </v-card-text>
                </v-card>

                <!-- Error区域 -->
                <v-card v-if="result2 === 'error'" flat class="mb-4" elevation="2" rounded="lg" outlined>
                  <v-card-title class="text-subtitle-1">エラー</v-card-title>
                  <v-card-text>
                    <v-card color="red-lighten-5">
                      <v-card-title class="text-error">エラーメッセージ一覧</v-card-title>
                      <v-card-text>
                        <v-list>
                          <v-list-item v-for="error in errors2" :key="error">
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
              </v-window-item>
            </v-window>
          </v-card-text>
        </v-card>
      </v-container>
    </v-main>
  </v-app>
</template>

<style scoped>
.action-btn {
  min-width: 120px;
  height: 40px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.action-btn:hover {
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.24);
}

/* Tab样式优化 */
:deep(.v-tabs) {
  padding: 10px 0;
}

:deep(.v-tab) {
  font-size: 16px;
  font-weight: 500;
  padding: 12px 24px;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 8px 8px 0 0;
  margin-right: 10px;
}

:deep(.v-tab:hover) {
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

:deep(.v-tab--active) {
  background-color: rgba(255, 255, 255, 0.35) !important;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
  font-weight: 600;
}

/* 移除滑块，因为活动tab已经有明显的样式 */
:deep(.v-tabs-slider) {
  display: none;
}

/* 链接样式优化 */
a {
  cursor: pointer;
  transition: color 0.3s ease;
}

a:hover {
  color: #1565c0 !important;
  text-decoration: underline !important;
}
</style>
