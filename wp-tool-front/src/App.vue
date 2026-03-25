<script setup lang="ts">
import { ref, computed } from 'vue'
import ScreenDesignTab from './components/ScreenDesignTab.vue'
import DBQueryTab from './components/DBQueryTab.vue'
import { getApiUrl } from './config/api'
import { MESSAGES } from './config/messages'

const tab = ref(0)

const files1 = ref<File[]>([])
const files2 = ref<File[]>([])
const result1 = ref<'idle' | 'parsing' | 'success' | 'error'>('idle')
const result2 = ref<'idle' | 'parsing' | 'success' | 'error'>('idle')
const errors1 = ref<string[]>([])
const errors2 = ref<string[]>([])
const apiResponse = ref<any>(null)

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

const generateCode1 = async () => {
  result1.value = 'parsing'
  errors1.value = []
  apiResponse.value = null
  
  try {
    const formData = new FormData()
    
    files1.value.forEach(file => {
      formData.append('ioFiles', file)
    })
    
    files2.value.forEach(file => {
      formData.append('dbQueryFiles', file)
    })
    
    const response = await fetch('/api/v1/excel/generate-io-code', {
      method: 'POST',
      body: formData
    })
    
    const result = await response.json()
    
    if (result.code === 200) {
      result1.value = 'success'
      apiResponse.value = result.data
    } else if (result.code === 400) {
      result1.value = 'error'
      errors1.value = [result.message]
    } else {
      result1.value = 'error'
      errors1.value = ['予期しないエラーが発生しました']
    }
  } catch (error) {
    result1.value = 'error'
    errors1.value = ['API呼び出し中にエラーが発生しました']
  }
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
  if (apiResponse.value && apiResponse.value.zipBase64) {
    const binaryString = atob(apiResponse.value.zipBase64)
    const bytes = new Uint8Array(binaryString.length)
    for (let i = 0; i < binaryString.length; i++) {
      bytes[i] = binaryString.charCodeAt(i)
    }
    const blob = new Blob([bytes], { type: 'application/zip' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${apiResponse.value.taskId}.zip`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  }
}

const clearFiles1 = () => {
  files1.value = []
  result1.value = 'idle'
  errors1.value = []
  apiResponse.value = null
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
      <v-container fluid max-width="none">
        <v-card elevation="0">
          <v-card-title class="text-h5 pa-4" style="background-color: #8E2DE2; color: white;">WP内部設計仕様書解析ツール</v-card-title>
          <v-card-text style="padding: 0;">
            <div style="background-color: #8E2DE2; padding: 10px 0 10px 0; margin: -20px -20px 0 -20px;">
              <v-tabs v-model="tab" style="background-color: #8E2DE2; padding: 0 20px;">
                <v-tab style="background-color: rgba(255, 255, 255, 0.2); color: white; margin-right: 10px; border-radius: 8px 8px 0 0;">画面仕様書</v-tab>
                <v-tab style="background-color: rgba(255, 255, 255, 0.2); color: white; margin-right: 10px; border-radius: 8px 8px 0 0;">DBQuery定義書</v-tab>
              </v-tabs>
            </div>

            <v-window v-model="tab" style="padding: 20px; width: 100%;">
              <v-window-item style="width: 100%;">
                <!-- 模版区域 -->
                <v-card flat class="mb-4" elevation="2" rounded="lg" outlined>
                  <v-card-title class="text-subtitle-1">テンプレート</v-card-title>
                  <hr style="border: 1px solid #e0e0e0; margin: 0 0 16px 0;">
                  <v-card-text>
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div>
                        <div class="d-flex items-center">
                          <a :href="`/templete/screen/${templateScreenName}`" download style="text-decoration: none; color: #1976d2;">
                            {{ templateScreenName }}
                          </a>
                        </div>
                      </div>
                      <div>
                        <div class="d-flex items-center">
                          <a :href="`/templete/dbquery/${templateDBQueryName}`" download style="text-decoration: none; color: #1976d2;">
                            {{ templateDBQueryName }}
                          </a>
                        </div>
                      </div>
                    </div>
                  </v-card-text>
                </v-card>

                <!-- 上传区域 -->
                <v-card flat class="mb-4" elevation="2" rounded="lg" outlined>
                  <v-card-title class="text-subtitle-1">アップロード</v-card-title>
                  <hr style="border: 1px solid #e0e0e0; margin: 0 0 16px 0;">
                  <v-card-text>
                    <!-- 上传说明 -->
                    <div class="mb-4">
                      <h4 class="mb-2">アップロードの説明</h4>
                      <ul class="list-disc pl-5" style="font-size: 14px;">
                        <li v-for="(instruction, index) in MESSAGES.UPLOAD_INSTRUCTIONS" :key="index" class="mb-1">{{ instruction }}</li>
                      </ul>
                    </div>
                    
                    <!-- 并排上传区域 -->
                    <v-row>
                      <v-col cols="12" md="6">
                        <h4 class="mb-2 d-flex items-center">
                          <span style="font-weight: bold;">
                            <span style="color: red; font-size: 1.4em; font-weight: bolder; vertical-align: baseline;">*</span>
                            画面仕様書
                          </span>
                        </h4>
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
                      </v-col>
                      
                      <v-col cols="12" md="6">
                        <h4 class="mb-2 d-flex items-center">
                          <span class="font-bold">DBQuery定義書</span>
                        </h4>
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
                      </v-col>
                    </v-row>
                    
                    <div class="d-flex justify-end mt-4">
                      <v-btn
                        @click="generateCode1"
                        color="primary"
                        class="action-btn mr-2"
                        :disabled="files1.length === 0"
                      >
                        コード生成
                      </v-btn>
                      <v-btn v-if="files1.length > 0 || files2.length > 0" color="warning" class="action-btn" @click="() => { clearFiles1(); clearFiles2(); }">
                        クリア
                      </v-btn>
                    </div>
                  </v-card-text>
                </v-card>

                <!-- 结果区域 -->
                <v-card v-if="result1 === 'parsing' || result1 === 'success'" flat class="mb-4" elevation="2" rounded="lg" outlined>
                  <v-card-title class="text-subtitle-1">結果</v-card-title>
                  <hr style="border: 1px solid #e0e0e0; margin: 0 0 16px 0;">
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
                        <v-btn v-if="apiResponse && apiResponse.taskId && apiResponse.zipBase64" color="primary" @click="downloadZip" class="mt-2">
                          <v-icon left>mdi-download</v-icon>
                          {{ apiResponse.taskId }}.ZIP をダウンロード
                        </v-btn>
                        <div v-else class="mt-2 text-gray-500">暂无可下载的文件</div>
                      </v-card-text>
                    </v-card>
                  </v-card-text>
                </v-card>

                <!-- Error区域 -->
                <v-card v-if="result1 === 'error'" flat class="mb-4" elevation="2" rounded="lg" outlined>
                  <v-card-title class="text-subtitle-1">エラー</v-card-title>
                  <hr style="border: 1px solid #e0e0e0; margin: 0 0 16px 0;">
                  <v-card-text>
                    <v-card color="red-lighten-5">
                      <v-card-title class="text-error">エラーメッセージ一覧</v-card-title>
                      <v-card-text>
                        <v-list>
                          <v-list-item v-for="error in errors1" :key="error">
                            <v-list-item-icon>
                              <v-icon color="error">mdi-alert-circle</v-icon>
                            </v-list-item-icon>
                            <v-list-item-title class="text-error" style="word-wrap: break-word; white-space: pre-wrap;">{{ error }}</v-list-item-title>
                          </v-list-item>
                        </v-list>
                      </v-card-text>
                    </v-card>
                    
                    <v-card v-if="apiResponse && apiResponse.errorLog && apiResponse.errorLog.length > 0" color="orange-lighten-5" class="mt-4">
                      <v-card-title class="text-orange-darken-2">エラーログ</v-card-title>
                      <v-card-text style="max-height: 300px; overflow-y: auto;">
                        <v-list density="compact">
                          <v-list-item v-for="(log, index) in apiResponse.errorLog" :key="'error-' + index">
                            <v-list-item-icon>
                              <v-icon color="orange-darken-2" size="small">mdi-alert</v-icon>
                            </v-list-item-icon>
                            <v-list-item-title class="text-body-2" style="word-wrap: break-word; white-space: pre-wrap;">{{ log }}</v-list-item-title>
                          </v-list-item>
                        </v-list>
                      </v-card-text>
                    </v-card>
                    
                    <v-card v-if="apiResponse && apiResponse.warnLog && apiResponse.warnLog.length > 0" color="yellow-lighten-5" class="mt-4">
                      <v-card-title class="text-yellow-darken-2">警告ログ</v-card-title>
                      <v-card-text style="max-height: 300px; overflow-y: auto;">
                        <v-list density="compact">
                          <v-list-item v-for="(log, index) in apiResponse.warnLog" :key="'warn-' + index">
                            <v-list-item-icon>
                              <v-icon color="yellow-darken-2" size="small">mdi-alert-outline</v-icon>
                            </v-list-item-icon>
                            <v-list-item-title class="text-body-2" style="word-wrap: break-word; white-space: pre-wrap;">{{ log }}</v-list-item-title>
                          </v-list-item>
                        </v-list>
                      </v-card-text>
                    </v-card>
                  </v-card-text>
                </v-card>
              </v-window-item>

              <v-window-item style="width: 100%;">
                <!-- 模版区域 -->
                <v-card flat class="mb-4" elevation="2" rounded="lg" outlined>
                  <v-card-title class="text-subtitle-1">テンプレート</v-card-title>
                  <hr style="border: 1px solid #e0e0e0; margin: 0 0 16px 0;">
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
                  <hr style="border: 1px solid #e0e0e0; margin: 0 0 16px 0;">
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
                <v-card v-if="result2 === 'parsing' || result2 === 'success'" flat class="mb-4" elevation="2" rounded="lg" outlined>
                  <v-card-title class="text-subtitle-1">結果</v-card-title>
                  <hr style="border: 1px solid #e0e0e0; margin: 0 0 16px 0;">
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
                        <v-btn v-if="apiResponse && apiResponse.zipBase64" color="primary" @click="downloadZip" class="mt-2">
                          <v-icon left>mdi-download</v-icon>
                          SOURCE.ZIP をダウンロード
                        </v-btn>
                        <div v-else class="mt-2 text-gray-500">暂无可下载的文件</div>
                      </v-card-text>
                    </v-card>
                  </v-card-text>
                </v-card>

                <!-- Error区域 -->
                <v-card v-if="result2 === 'error'" flat class="mb-4" elevation="2" rounded="lg" outlined>
                  <v-card-title class="text-subtitle-1">エラー</v-card-title>
                  <hr style="border: 1px solid #e0e0e0; margin: 0 0 16px 0;">
                  <v-card-text>
                    <v-card color="red-lighten-5">
                      <v-card-title class="text-error">エラーメッセージ一覧</v-card-title>
                      <v-card-text>
                        <v-list>
                          <v-list-item v-for="error in errors2" :key="error">
                            <v-list-item-icon>
                              <v-icon color="error">mdi-alert-circle</v-icon>
                            </v-list-item-icon>
                            <v-list-item-title class="text-error" style="word-wrap: break-word; white-space: pre-wrap;">{{ error }}</v-list-item-title>
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
