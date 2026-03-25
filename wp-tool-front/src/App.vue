<script setup lang="ts">
import { ref, computed } from 'vue'
import ScreenDesignTab from './components/ScreenDesignTab.vue'
import DBQueryTab from './components/DBQueryTab.vue'
import { API_CONFIG, getApiUrl } from './config/api'
import { MESSAGES } from './config/messages'
const tab = ref(0)

//画面仕様書タブの変数
const screenFiles = ref<File[]>([])
const dbQueryFiles = ref<File[]>([])
const screenResult = ref<'idle' | 'parsing' | 'success' | 'error'>('idle')
const screenErrors = ref<string[]>([])
const apiScreenResponse = ref<any>(null)

//DBQuery定義書タブの変数
const dbQueryFiles2 = ref<File[]>([])
const dbQueryResult = ref<'idle' | 'parsing' | 'success' | 'error'>('idle')
const dbQueryErrors = ref<string[]>([])
const apiDbQueryResponse = ref<any>(null)


const addScreenFiles = (newFiles: File[]) => {
  screenFiles.value = [...screenFiles.value, ...newFiles]
}

const addDbQueryFiles = (newFiles: File[]) => {
  dbQueryFiles.value = [...dbQueryFiles.value, ...newFiles]
}

const addDbQueryFiles2 = (newFiles: File[]) => {
  dbQueryFiles2.value = [...dbQueryFiles2.value, ...newFiles]
}

const removeScreenFiles = (index: number) => {
  screenFiles.value.splice(index, 1)
}

const removeDbQueryFiles = (index: number) => {
  dbQueryFiles.value.splice(index, 1)
}
const removeDbQueryFiles2 = (index: number) => {
  dbQueryFiles2.value.splice(index, 1)
}

/**
 * 画面仕様書からWPコードを生成する
 */
const generateScreenWpCode= async () => {
  if (!confirm(MESSAGES.CONFIRMATION.GENERATE_CODE)) {
    return
  }

  
  screenResult.value = 'parsing'
  screenErrors.value = []
  apiScreenResponse.value = null
  
  try {
    const formData = new FormData()
    
    screenFiles.value.forEach(file => {
      formData.append('ioFiles', file)
    })
    
    dbQueryFiles.value.forEach(file => {
      formData.append('dbQueryFiles', file)
    })
    const response = await fetch(API_CONFIG.ENDPOINTS.GENERATE_IO_CODE, {
      method: 'POST',
      body: formData
    })
    
     // Scroll to results area immediately after confirmation
    const resultsSection = document.querySelector('.results-section')
    if (resultsSection) {
      resultsSection.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }

    const result = await response.json()
    
    if (result.code === 200) {
      screenResult.value = 'success'
      apiScreenResponse.value = result.data
    } else if (result.code === 400) {
      screenResult.value = 'error'
      screenErrors.value = [result.message]
    } else {
      screenResult.value = 'error'
      screenErrors.value = ['予期しないエラーが発生しました']
    }
  } catch (error) {
    screenResult.value = 'error'
    screenErrors.value = ['API呼び出し中にエラーが発生しました']
  }
}

/**
 * DBQuery定義書からWPコードを生成する
 */
const generateDbQueryWpCode = () => {
  alert('todo: download zip file')
}

const downloadZip = () => {
  if (apiScreenResponse.value && apiScreenResponse.value.zipBase64) {
    const binaryString = atob(apiScreenResponse.value.zipBase64)
    const bytes = new Uint8Array(binaryString.length)
    for (let i = 0; i < binaryString.length; i++) {
      bytes[i] = binaryString.charCodeAt(i)
    }
    const blob = new Blob([bytes], { type: 'application/zip' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${apiScreenResponse.value.taskId}.zip`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  }

  if (apiDbQueryResponse.value && apiDbQueryResponse.value.zipBase64) {
    alert('todo: download zip file')
  }
}

const clearScreenFiles = () => {
  if (!confirm(MESSAGES.CONFIRMATION.CLEAR)) {
    return
  }
  screenFiles.value = []
  screenResult.value = 'idle'
  screenErrors.value = []
  apiScreenResponse.value = null
}

const templateScreenName = '（内部設計書サンプル_カード）画面設計書.xlsx'
const templateDBQueryName = '（内部設計書サンプル_カード）dbQuery定義書.xlsx'

const clearDbQueryFiles = () => {
  dbQueryFiles.value = []
  dbQueryResult.value = 'idle'
  dbQueryErrors.value = []
}

const handleClear = () => {
  if (confirm(MESSAGES.CONFIRMATION.CLEAR)) {
    clearScreenFiles()
    clearDbQueryFiles()
  }
}
</script>

<template>
  <div style="background-color: #f5f5f5; min-height: 100vh; padding: 0; margin: 0; font-family: Arial, sans-serif;">
    <!-- メインコンテナ -->
    <div style="width: 75%; margin: 0 auto; background-color: white; box-shadow: 0 0 10px rgba(0,0,0,0.1); min-width: 900px;">
      <!-- ヘッダー -->
      <div style="background-color: #8E2DE2; color: white; padding: 20px;">
        <h1 style="margin: 0; font-size: 20px;">WP内部設計仕様書解析ツール</h1>
      </div>
      
      <!-- タブ -->
      <div style="background-color: #8E2DE2; padding: 10px 20px;">
        <div style="display: flex;">
          <div 
            @click="tab = 0" 
            style="background-color: rgba(255, 255, 255, 0.2); color: white; margin-right: 10px; border-radius: 8px 8px 0 0; padding: 12px 24px; cursor: pointer; font-size: 16px;"
            :style="{ backgroundColor: tab === 0 ? 'rgba(255, 255, 255, 0.35)' : 'rgba(255, 255, 255, 0.2)' }"
          >
            画面仕様書
          </div>
          <div 
            @click="tab = 1" 
            style="background-color: rgba(255, 255, 255, 0.2); color: white; margin-right: 10px; border-radius: 8px 8px 0 0; padding: 12px 24px; cursor: pointer; font-size: 16px;"
            :style="{ backgroundColor: tab === 1 ? 'rgba(255, 255, 255, 0.35)' : 'rgba(255, 255, 255, 0.2)' }"
          >
            DBQuery定義書
          </div>
          <div 
            @click="tab = 2" 
            style="background-color: rgba(255, 255, 255, 0.2); color: white; margin-right: 10px; border-radius: 8px 8px 0 0; padding: 12px 24px; cursor: pointer; font-size: 16px;"
            :style="{ backgroundColor: tab === 2 ? 'rgba(255, 255, 255, 0.35)' : 'rgba(255, 255, 255, 0.2)' }"
          >
            テンプレート
          </div>
        </div>
      </div>

      <!-- コンテンツ -->
      <div style="padding: 20px;">
        <!-- 画面仕様書タブ -->
        <div v-if="tab === 0">
          <!-- 上传区域 -->
          <div class="mb-4" style="border: 1px solid #e0e0e0; border-radius: 8px; padding: 16px;">
            <!-- 上传说明 -->
            <div class="mb-4">
              <h4 class="mb-2">アップロードの説明</h4>
              <ul class="list-disc pl-5" style="font-size: 14px;">
                <li v-for="(instruction, index) in MESSAGES.UPLOAD_SCREEN_INSTRUCTIONS" :key="index" class="mb-1">{{ instruction }}</li>
              </ul>
            </div>
            
            <hr style="border: 1px solid #e0e0e0; margin: 0 0 16px 0;">
            
            <!-- 并排上传区域 -->
            <div style="display: flex; gap: 20px; width: 100%;">
              <div style="flex: 1; display: flex; flex-direction: column;">
                <h4 class="mb-2">
                  <span style="color: red; font-weight: bolder; vertical-align: baseline;">*</span>
                  <span style="font-weight: bold;">
                    画面仕様書
                  </span>
                </h4>
                <ScreenDesignTab @files-added="addScreenFiles" />
                <div v-if="screenFiles.length > 0" class="mt-4">
                  <h5>アップロードされたファイル</h5>
                  <div v-for="(file, index) in screenFiles" :key="file.name + index" style="display: flex; justify-content: space-between; padding: 8px; border-bottom: 1px solid #e0e0e0;">
                    <span>{{ file.name }}</span>
                    <button @click="removeScreenFiles(index)" style="background: none; border: none; color: red; cursor: pointer;">×</button>
                  </div>
                </div>
              </div>
              
              <div style="flex: 1; display: flex; flex-direction: column;">
                <h4 class="mb-2">
                  <span style="font-weight: bold;">DBQuery定義書</span>
                </h4>
                <DBQueryTab @files-added="addDbQueryFiles" />
                <div v-if="dbQueryFiles.length > 0" class="mt-4">
                  <h5>アップロードされたファイル</h5>
                  <div v-for="(file, index) in dbQueryFiles" :key="file.name + index" style="display: flex; justify-content: space-between; padding: 8px; border-bottom: 1px solid #e0e0e0;">
                    <span>{{ file.name }}</span>
                    <button @click="removeDbQueryFiles(index)" style="background: none; border: none; color: red; cursor: pointer;">×</button>
                  </div>
                </div>
              </div>
            </div>
            
            <hr style="border: 1px solid #e0e0e0; margin: 20px 0;">
            
            <div style="display: flex; justify-content: flex-end; gap: 10px;">
              <button
                @click="generateScreenWpCode"
                :style="{ backgroundColor: screenFiles.length === 0 ? '#ccc' : '#1976d2', color: 'white', border: 'none', padding: '8px 16px', borderRadius: '4px', cursor: screenFiles.length === 0 ? 'not-allowed' : 'pointer' }"
                :disabled="screenFiles.length === 0"
              >
                コード生成
              </button>
              <button v-if="screenFiles.length > 0 || dbQueryFiles.length > 0" style="background-color: #ffc107; color: black; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer;" @click="handleClear">
                クリア
              </button>
            </div>
          </div>

          <!-- 结果区域 -->
          <div v-if="screenResult === 'parsing' || screenResult === 'success'" class="mb-4 results-section" style="border: 1px solid #e0e0e0; border-radius: 8px; padding: 16px;">
            <h3 style="margin-top: 0; margin-bottom: 16px;">結果</h3>
            <hr style="border: 1px solid #e0e0e0; margin: 0 0 16px 0;">
            
            <div v-if="screenResult === 'parsing'" style="background-color: #e3f2fd; padding: 16px; border-radius: 4px; text-align: center;">
              <div>解析中...</div>
            </div>
            <div v-if="screenResult === 'success'" style="background-color: #e8f5e8; padding: 16px; border-radius: 4px; text-align: center;">
              <div style="color: green; font-weight: bold;">解析成功</div>
              <button v-if="apiScreenResponse && apiScreenResponse.taskId && apiScreenResponse.zipBase64" style="background-color: #1976d2; color: white; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; margin-top: 16px;" @click="downloadZip">
                {{ apiScreenResponse.taskId }}.ZIP をダウンロード
              </button>
              <div v-else style="margin-top: 16px; color: #666;">暂无可下载的文件</div>
            </div>
          </div>

          <!-- Error区域 -->
          <div v-if="screenResult === 'error' || (screenResult === 'success' && apiScreenResponse && (apiScreenResponse.errorLog && apiScreenResponse.errorLog.length > 0 || apiScreenResponse.warnLog && apiScreenResponse.warnLog.length > 0))" class="mb-4 results-section" style="border: 1px solid #e0e0e0; border-radius: 8px; padding: 16px;">
            <h3 style="margin-top: 0; margin-bottom: 16px;">エラー</h3>
            <hr style="border: 1px solid #e0e0e0; margin: 0 0 16px 0;">
            
            <div v-if="screenResult === 'error'" style="background-color: #ffebee; padding: 16px; border-radius: 4px; margin-bottom: 16px;">
              <h4 style="margin-top: 0; color: #c62828;">エラーメッセージ一覧</h4>
              <ul style="list-style-type: disc; padding-left: 20px;">
                <li v-for="error in screenErrors" :key="error" style="color: #c62828; word-wrap: break-word; white-space: pre-wrap;">{{ error }}</li>
              </ul>
            </div>
            
            <div v-if="apiScreenResponse && apiScreenResponse.errorLog && apiScreenResponse.errorLog.length > 0" style="background-color: #fff3e0; padding: 16px; border-radius: 4px; margin-bottom: 16px;">
              <h4 style="margin-top: 0; color: #ef6c00;">エラーログ</h4>
              <div style="max-height: 300px; overflow-y: auto;">
                <ul style="list-style-type: disc; padding-left: 20px;">
                  <li v-for="(log, index) in apiScreenResponse.errorLog" :key="'error-' + index" style="font-size: 14px; word-wrap: break-word; white-space: pre-wrap;">{{ log }}</li>
                </ul>
              </div>
            </div>
            
            <div v-if="apiScreenResponse && apiScreenResponse.warnLog && apiScreenResponse.warnLog.length > 0" style="background-color: #fff8e1; padding: 16px; border-radius: 4px;">
              <h4 style="margin-top: 0; color: #f57c00;">警告ログ</h4>
              <div style="max-height: 300px; overflow-y: auto;">
                <ul style="list-style-type: disc; padding-left: 20px;">
                  <li v-for="(log, index) in apiScreenResponse.warnLog" :key="'warn-' + index" style="font-size: 14px; word-wrap: break-word; white-space: pre-wrap;">{{ log }}</li>
                </ul>
              </div>
            </div>
          </div>
        </div>

        <!-- DBQuery定義書タブ -->
        <div v-if="tab === 1">
          <!-- 上传区域 -->
          <div class="mb-4" style="border: 1px solid #e0e0e0; border-radius: 8px; padding: 16px;">
            <!-- 上传说明 -->
            <div class="mb-4">
              <h4 class="mb-2">アップロードの説明</h4>
              <ul class="list-disc pl-5" style="font-size: 14px;">
                <li v-for="(instruction, index) in MESSAGES.UPLOAD_DBQUERY_INSTRUCTIONS" :key="index" class="mb-1">{{ instruction }}</li>
              </ul>
            </div>
            
            <hr style="border: 1px solid #e0e0e0; margin: 0 0 16px 0;">
            
            <div style="display: flex; flex-direction: column; height: 100%;">
              <DBQueryTab @files-added="addDbQueryFiles2" />
              <div v-if="dbQueryFiles2.length > 0" class="mt-4">
                <h5>アップロードされたファイル</h5>
                <div v-for="(file, index) in dbQueryFiles2" :key="file.name + index" style="display: flex; justify-content: space-between; padding: 8px; border-bottom: 1px solid #e0e0e0;">
                  <span>{{ file.name }}</span>
                  <button @click="removeDbQueryFiles2(index)" style="background: none; border: none; color: red; cursor: pointer;">×</button>
                </div>
              </div>
            </div>
            
            <hr style="border: 1px solid #e0e0e0; margin: 20px 0;">
            
            <div style="display: flex; justify-content: flex-end; gap: 10px;">
              <button
                @click="generateDbQueryWpCode"
                style="background-color: #1976d2; color: white; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer;"
                :disabled="dbQueryFiles2.length === 0"
              >
                コード生成
              </button>
              <button v-if="dbQueryFiles2.length > 0" style="background-color: #ffc107; color: black; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer;" @click="clearDbQueryFiles">
                クリア
              </button>
            </div>
          </div>

          <!-- 结果区域 -->
          <div v-if="dbQueryResult === 'parsing' || dbQueryResult === 'success'" class="mb-4" style="border: 1px solid #e0e0e0; border-radius: 8px; padding: 16px;">
            <h3 style="margin-top: 0; margin-bottom: 16px;">結果</h3>
            <hr style="border: 1px solid #e0e0e0; margin: 0 0 16px 0;">
            
            <div v-if="dbQueryResult === 'parsing'" style="background-color: #e3f2fd; padding: 16px; border-radius: 4px; text-align: center;">
              <div>解析中...</div>
            </div>
            <div v-if="dbQueryResult === 'success'" style="background-color: #e8f5e8; padding: 16px; border-radius: 4px; text-align: center;">
              <div style="color: green; font-weight: bold;">解析成功</div>
              <button v-if="apiScreenResponse && apiScreenResponse.zipBase64" style="background-color: #1976d2; color: white; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; margin-top: 16px;" @click="downloadZip">
                SOURCE.ZIP をダウンロード
              </button>
              <div v-else style="margin-top: 16px; color: #666;">暂无可下载的文件</div>
            </div>
          </div>

          <!-- Error区域 -->
          <div v-if="dbQueryResult === 'error'" class="mb-4" style="border: 1px solid #e0e0e0; border-radius: 8px; padding: 16px;">
            <h3 style="margin-top: 0; margin-bottom: 16px;">エラー</h3>
            <hr style="border: 1px solid #e0e0e0; margin: 0 0 16px 0;">
            
            <div style="background-color: #ffebee; padding: 16px; border-radius: 4px;">
              <h4 style="margin-top: 0; color: #c62828;">エラーメッセージ一覧</h4>
              <ul style="list-style-type: disc; padding-left: 20px;">
                <li v-for="error in dbQueryErrors" :key="error" style="color: #c62828; word-wrap: break-word; white-space: pre-wrap;">{{ error }}</li>
              </ul>
            </div>
          </div>
        </div>

        <!-- テンプレートダウンロードタブ -->
        <div v-if="tab === 2">
          <!-- テンプレートダウンロード区域 -->
          <div class="mb-4" style="border: 1px solid #e0e0e0; border-radius: 8px; padding: 16px;">
            <h3 style="margin-top: 0; margin-bottom: 16px;">画面設計書</h3>
            <hr style="border: 1px solid #e0e0e0; margin: 0 0 16px 0;">
            <div style="display: flex; flex-wrap: wrap; gap: 20px;">
              <div>
                <a :href="`/templete/screen/${templateScreenName}`" download style="text-decoration: none; color: #1976d2;">
                  {{ templateScreenName }}
                </a>
              </div>
            </div>
          </div>
           <div class="mb-4" style="border: 1px solid #e0e0e0; border-radius: 8px; padding: 16px;">
            <h3 style="margin-top: 0; margin-bottom: 16px;">DBQuery定義書</h3>
            <hr style="border: 1px solid #e0e0e0; margin: 0 0 16px 0;">
            <div style="display: flex; flex-wrap: wrap; gap: 20px;">
              <div>
                <a :href="`/templete/dbquery/${templateDBQueryName}`" download style="text-decoration: none; color: #1976d2;">
                  {{ templateDBQueryName }}
                </a>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
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
